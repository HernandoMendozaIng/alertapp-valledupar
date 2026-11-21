package co.edu.unad.alertapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU 02 - Criterio de aceptación "entrega de alerta con latencia de 5 s o menos".
 * Levanta el servidor en un puerto real, abre el canal SSE como lo hace la app y mide cuánto tarda en llegar
 * la alerta desde que otro ciudadano envía el reporte.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LatenciaAlertasTest {

    @LocalServerPort int puerto;
    @Autowired ObjectMapper json;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    @DisplayName("PF-25 La alerta de un reporte cercano llega por el canal en tiempo real en menos de 5 s")
    void latencia() throws Exception {
        String autora = registrar("Emisora");
        String vecina = registrar("Receptora");
        enviar("PUT", "/api/auth/yo/ubicacion", vecina, "{\"latitud\":10.4545,\"longitud\":-73.2530}");

        HttpResponse<java.io.InputStream> flujo = http.send(HttpRequest.newBuilder(uri("/api/notificaciones/flujo"))
                .header("Authorization", "Bearer " + vecina).header("Accept", "text/event-stream").build(),
                HttpResponse.BodyHandlers.ofInputStream());
        BufferedReader lector = new BufferedReader(new InputStreamReader(flujo.body(), StandardCharsets.UTF_8));
        CompletableFuture<Long> llegada = CompletableFuture.supplyAsync(() -> {
            try {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.equals("event:notificacion")) return System.nanoTime();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return -1L;
        });

        String fecha = OffsetDateTime.now(ZoneOffset.ofHours(-5)).minusMinutes(10).withNano(0).toString();
        long inicio = System.nanoTime();
        enviar("POST", "/api/reportes", autora, """
                {"tipoDelito":"HURTO","arma":"SIN_ARMA","medio":"A_PIE","elementoHurtado":"CELULAR",
                 "descripcion":"Prueba de latencia","fechaHecho":"%s","latitud":10.4540,"longitud":-73.2529,
                 "comuna":3,"barrio":"Primero de Mayo","anonimo":true}""".formatted(fecha));
        long fin = llegada.get(5, TimeUnit.SECONDS);
        double segundos = (fin - inicio) / 1e9;
        System.out.printf("Latencia de la alerta: %.3f s%n", segundos);
        assertThat(segundos).isLessThan(5.0);
        flujo.body().close();
    }

    private String registrar(String nombre) throws Exception {
        String correo = nombre.toLowerCase() + "." + UUID.randomUUID().toString().substring(0, 8) + "@prueba.co";
        String r = enviar("POST", "/api/auth/registro", null, json.writeValueAsString(Map.of(
                "nombre", nombre, "correo", correo, "clave", "ClaveSegura1", "aceptoPolitica", true)));
        return json.readTree(r).get("token").asText();
    }

    private String enviar(String metodo, String ruta, String token, String cuerpo) throws Exception {
        var b = HttpRequest.newBuilder(uri(ruta)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as(ruta + " " + r.body()).isBetween(200, 299);
        return r.body();
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
