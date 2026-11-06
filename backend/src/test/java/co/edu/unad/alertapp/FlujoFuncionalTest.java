package co.edu.unad.alertapp;

import co.edu.unad.alertapp.usuario.Rol;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales de extremo a extremo sobre la API (criterio TRL 5 acordado con el tutor):
 * cada operación se ejecuta como lo haría la app y se verifica que el dato quede guardado y se pueda consultar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FlujoFuncionalTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired PasswordEncoder codificador;

    private String tokenAutora;

    @BeforeEach
    void preparar() throws Exception {
        tokenAutora = registrar("Ana Villazón");
    }

    @Test
    @DisplayName("PF-01 Registro e ingreso: crea la cuenta, inicia sesión y consulta el perfil")
    void registroEIngreso() throws Exception {
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.villazon@prueba.co\",\"clave\":\"ClaveSegura1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("CIUDADANO"));
        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Villazón"));
    }

    @Test
    @DisplayName("PF-02 Validaciones: rechaza correo repetido, clave débil y política no aceptada")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"correo\":\"ana.villazon@prueba.co\",\"clave\":\"ClaveSegura1\",\"aceptoPolitica\":true}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pepe\",\"correo\":\"pepe@prueba.co\",\"clave\":\"corta\",\"aceptoPolitica\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.clave").exists())
                .andExpect(jsonPath("$.campos.aceptoPolitica").exists());
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.villazon@prueba.co\",\"clave\":\"incorrecta1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PF-03 Reporte guiado: guarda, clasifica severidad y aparece en el mapa y en mis reportes")
    void crearYConsultarReporte() throws Exception {
        long id = crearReporte(tokenAutora);
        mvc.perform(get("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severidad").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.autor").doesNotExist())
                .andExpect(jsonPath("$.esMio").value(true));
        mvc.perform(get("/api/reportes").param("lat", "10.4540").param("lng", "-73.2529").param("radio", "200")
                        .header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/reportes").param("lat", "10.4900").param("lng", "-73.2300").param("radio", "200")
                        .header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/reportes/mios").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    @DisplayName("PF-04 Reporte con foto: acepta JPG y rechaza archivos que no son imagen")
    void reporteConFoto() throws Exception {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
        var datos = new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, cuerpoReporte().getBytes());
        var foto = new MockMultipartFile("foto", "evidencia.jpg", "image/jpeg", jpg);
        String r = mvc.perform(multipart("/api/reportes").file(datos).file(foto).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tieneEvidencia").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(r).get("id").asLong();
        mvc.perform(get("/api/reportes/" + id + "/evidencia").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));

        var falsa = new MockMultipartFile("foto", "virus.jpg", "image/jpeg", "MZ esto no es una foto".getBytes());
        mvc.perform(multipart("/api/reportes").file(datos).file(falsa).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("PF-05 Actualizar y eliminar: solo el autor y solo mientras está pendiente")
    void actualizarYEliminar() throws Exception {
        long id = crearReporte(tokenAutora);
        String editado = cuerpoReporte().replace("ARMA_FUEGO", "SIN_ARMA").replace("\"ATRACO\"", "\"HURTO\"");
        mvc.perform(put("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora)
                        .contentType(MediaType.APPLICATION_JSON).content(editado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arma").value("SIN_ARMA"))
                .andExpect(jsonPath("$.severidad").value("MEDIA"));
        String otro = registrar("Beto Araújo");
        mvc.perform(delete("/api/reportes/" + id).header("Authorization", "Bearer " + otro))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PF-06 Validación comunitaria: tres confirmaciones validan el reporte y bloquean su edición")
    void validacionComunitaria() throws Exception {
        long id = crearReporte(tokenAutora);
        mvc.perform(put("/api/reportes/" + id + "/voto").header("Authorization", "Bearer " + tokenAutora)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"CONFIRMA\"}"))
                .andExpect(status().isConflict());
        for (int i = 0; i < 3; i++) {
            votar(registrar("Vecino " + i), id, "CONFIRMA").andExpect(status().isOk());
        }
        mvc.perform(get("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.confirmaciones").value(3));
        mvc.perform(put("/api/reportes/" + id).header("Authorization", "Bearer " + tokenAutora)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpoReporte()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PF-07 Anti-spam: tres votos de falso anulan el reporte y cambiar el voto recalcula")
    void antiSpam() throws Exception {
        long id = crearReporte(tokenAutora);
        String v1 = registrar("Vecina Uno");
        votar(v1, id, "CONFIRMA").andExpect(jsonPath("$.confirmaciones").value(1));
        votar(v1, id, "FALSO").andExpect(jsonPath("$.confirmaciones").value(0)).andExpect(jsonPath("$.falsos").value(1));
        votar(registrar("Vecina Dos"), id, "FALSO");
        votar(registrar("Vecina Tres"), id, "FALSO").andExpect(jsonPath("$.estado").value("DESCARTADO"));
        votar(registrar("Vecina Cuatro"), id, "CONFIRMA").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PF-08 Comentarios: se guardan y se listan en orden con el primer nombre del vecino")
    void comentarios() throws Exception {
        long id = crearReporte(tokenAutora);
        String vecino = registrar("Carlos Mario Pumarejo");
        mvc.perform(post("/api/reportes/" + id + "/comentarios").header("Authorization", "Bearer " + vecino)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Yo vi la moto, era roja.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/reportes/" + id + "/comentarios").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$[0].autor").value("Carlos"))
                .andExpect(jsonPath("$[0].texto").value("Yo vi la moto, era roja."));
    }

    @Test
    @DisplayName("PF-09 Panel de autoridad: cambia el estado, deja nota y el ciudadano no tiene acceso")
    void panelAutoridad() throws Exception {
        long id = crearReporte(tokenAutora);
        mvc.perform(patch("/api/panel/reportes/" + id + "/estado").header("Authorization", "Bearer " + tokenAutora)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_REVISION\"}"))
                .andExpect(status().isForbidden());
        String autoridad = tokenAutoridad();
        mvc.perform(patch("/api/panel/reportes/" + id + "/estado").header("Authorization", "Bearer " + autoridad)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_REVISION\",\"nota\":\"Se asignó el caso al cuadrante 12.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        mvc.perform(patch("/api/panel/reportes/" + id + "/estado").header("Authorization", "Bearer " + autoridad)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/reportes/" + id + "/comentarios").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$[0].esAutoridad").value(true));
        mvc.perform(get("/api/analitica/exportar").header("Authorization", "Bearer " + autoridad))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("alertapp-reportes-")));
        mvc.perform(get("/api/analitica/exportar").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PF-10 Botón de pánico: crea la alerta una sola vez, la autoridad la ve y la atiende")
    void botonDePanico() throws Exception {
        String cuerpo = "{\"latitud\":10.4540,\"longitud\":-73.2529}";
        long id = leerId(mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenAutora)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated()));
        mvc.perform(post("/api/alertas").header("Authorization", "Bearer " + tokenAutora)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(jsonPath("$.id").value(id));
        String autoridad = tokenAutoridad();
        mvc.perform(get("/api/panel/alertas").header("Authorization", "Bearer " + autoridad))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
        mvc.perform(post("/api/panel/alertas/" + id + "/atender").header("Authorization", "Bearer " + autoridad)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":\"Patrulla en el sitio\"}"))
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
        mvc.perform(post("/api/alertas/" + id + "/cancelar").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PF-11 Analítica: métricas, hotspots e índice de riesgo responden aunque la IA esté apagada")
    void analiticaConRespaldo() throws Exception {
        for (int i = 0; i < 5; i++) crearReporte(tokenAutora);
        mvc.perform(get("/api/analitica/metricas").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.porHora.length()").value(24));
        mvc.perform(get("/api/analitica/hotspots").header("Authorization", "Bearer " + tokenAutora))
                .andExpect(jsonPath("$.motor").value("java-respaldo"))
                .andExpect(jsonPath("$.hotspots.length()").value(1))
                .andExpect(jsonPath("$.hotspots[0].reportes").value(5));
        String riesgo = mvc.perform(get("/api/analitica/riesgo").param("lat", "10.4540").param("lng", "-73.2529")
                        .header("Authorization", "Bearer " + tokenAutora))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(riesgo).get("indice").asDouble()).isGreaterThan(0);
    }

    @Test
    @DisplayName("PF-13 Fuerza bruta: tras 5 claves erradas el ingreso se bloquea aunque la clave sea correcta")
    void limiteDeIntentos() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"ana.villazon@prueba.co\",\"clave\":\"mala" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana.villazon@prueba.co\",\"clave\":\"ClaveSegura1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("PF-12 Seguridad: sin token no hay acceso y un token con el rol alterado se rechaza")
    void seguridad() throws Exception {
        mvc.perform(get("/api/reportes")).andExpect(status().isUnauthorized());
        String[] partes = tokenAutora.split("\\.");
        var decodificador = java.util.Base64.getUrlDecoder();
        var codificadorB64 = java.util.Base64.getUrlEncoder().withoutPadding();
        String carga = new String(decodificador.decode(partes[1])).replace("CIUDADANO", "AUTORIDAD");
        String falsificado = partes[0] + "." + codificadorB64.encodeToString(carga.getBytes()) + "." + partes[2];
        mvc.perform(get("/api/panel/alertas").header("Authorization", "Bearer " + falsificado))
                .andExpect(status().isUnauthorized());
    }

    // ---- utilidades ----

    private String registrar(String nombre) throws Exception {
        String correo = nombre.toLowerCase().replace(' ', '.').replace("ó", "o").replace("ú", "u") + "@prueba.co";
        if (nombre.startsWith("Vecin")) correo = UUID.randomUUID() + "@prueba.co";
        String cuerpo = json.writeValueAsString(java.util.Map.of(
                "nombre", nombre, "correo", correo, "clave", "ClaveSegura1", "comuna", 3, "aceptoPolitica", true));
        String r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    private String tokenAutoridad() throws Exception {
        usuarios.save(new Usuario("Teniente Prueba", "teniente@prueba.co", codificador.encode("ClaveSegura1"),
                null, Rol.AUTORIDAD, null));
        String r = mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"teniente@prueba.co\",\"clave\":\"ClaveSegura1\"}")).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("token").asText();
    }

    private String cuerpoReporte() {
        String fecha = OffsetDateTime.now(ZoneOffset.ofHours(-5)).minusHours(2).withNano(0).toString();
        return """
                {"tipoDelito":"ATRACO","arma":"ARMA_FUEGO","medio":"MOTOCICLETA","elementoHurtado":"CELULAR",
                 "descripcion":"Dos hombres en moto me quitaron el celular.","fechaHecho":"%s",
                 "latitud":10.4540,"longitud":-73.2529,"comuna":3,"barrio":"Primero de Mayo","anonimo":true}
                """.formatted(fecha);
    }

    private long crearReporte(String token) throws Exception {
        return leerId(mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoReporte())).andExpect(status().isCreated()));
    }

    private ResultActions votar(String token, long id, String tipo) throws Exception {
        return mvc.perform(put("/api/reportes/" + id + "/voto").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"" + tipo + "\"}"));
    }

    private long leerId(ResultActions r) throws Exception {
        JsonNode n = json.readTree(r.andReturn().getResponse().getContentAsString());
        return n.get("id").asLong();
    }
}
