package co.edu.unad.alertapp;

import co.edu.unad.alertapp.seguridad.Totp;
import co.edu.unad.alertapp.usuario.Mensajero;
import co.edu.unad.alertapp.usuario.Rol;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales del Sprint 2 (HU 02, 04, 06, 10, 12 y recuperación de clave de la HU 01).
 * Cada prueba crea sus propios usuarios con correos únicos, así que no dependen entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadPersonalTest {

    /** Captura los mensajes que en producción irían por correo o SMS. */
    @TestConfiguration
    static class BuzonPrueba {
        static final Map<String, String> ULTIMO = new ConcurrentHashMap<>();

        @Bean
        @Primary
        Mensajero buzon() {
            return (destino, asunto, texto) -> ULTIMO.put(destino, texto);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired PasswordEncoder codificador;

    record Cuenta(String correo, String token, long id) {}

    // ---------------- HU 02 ----------------

    @Test
    @DisplayName("PF-14 Alerta geolocalizada: avisa al vecino a 150 m, no al lejano ni al que las apagó")
    void alertaCercana() throws Exception {
        Cuenta autora = cuenta("Autora");
        Cuenta cerca = cuenta("Cerca");
        Cuenta lejos = cuenta("Lejos");
        Cuenta apagado = cuenta("Apagado");
        ubicar(cerca, 10.4553, -73.2529);
        ubicar(lejos, 10.4700, -73.2400);
        ubicar(apagado, 10.4541, -73.2530);
        mvc.perform(conToken(put("/api/auth/yo/preferencias"), apagado)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notificaciones\":false,\"radioAlertas\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificaciones").value(false));

        crearReporte(autora);

        mvc.perform(conToken(get("/api/notificaciones"), cerca))
                .andExpect(jsonPath("$.sinLeer").value(1))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("REPORTE_CERCANO"))
                .andExpect(jsonPath("$.notificaciones[0].titulo").value("Atraco a 150 m de ti"));
        mvc.perform(conToken(get("/api/notificaciones"), lejos)).andExpect(jsonPath("$.sinLeer").value(0));
        mvc.perform(conToken(get("/api/notificaciones"), apagado)).andExpect(jsonPath("$.sinLeer").value(0));
        mvc.perform(conToken(get("/api/notificaciones"), autora)).andExpect(jsonPath("$.sinLeer").value(0));

        mvc.perform(conToken(post("/api/notificaciones/leidas"), cerca)).andExpect(status().isNoContent());
        mvc.perform(conToken(get("/api/notificaciones"), cerca)).andExpect(jsonPath("$.sinLeer").value(0));
    }

    @Test
    @DisplayName("PF-15 Preferencias: el radio de alerta solo acepta entre 200 m y 2 km")
    void radioValido() throws Exception {
        Cuenta c = cuenta("Radio");
        mvc.perform(conToken(put("/api/auth/yo/preferencias"), c)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notificaciones\":true,\"radioAlertas\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.radioAlertas").exists());
    }

    // ---------------- HU 04 ----------------

    @Test
    @DisplayName("PF-16 Ruta segura: calcula ruta segura y corta en los tres modos y rechaza puntos fuera del área")
    void rutaSegura() throws Exception {
        Cuenta c = cuenta("Ruta");
        for (String modo : new String[]{"A_PIE", "BICICLETA", "VEHICULO"}) {
            String r = mvc.perform(conToken(get("/api/rutas/segura"), c)
                            .param("origenLat", "10.4569").param("origenLng", "-73.2562")
                            .param("destinoLat", "10.4486").param("destinoLng", "-73.2464").param("modo", modo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modo").value(modo))
                    .andReturn().getResponse().getContentAsString();
            JsonNode j = json.readTree(r);
            assertThat(j.get("segura").get("puntos").size()).isGreaterThan(5);
            assertThat(j.get("segura").get("metros").asInt()).isGreaterThanOrEqualTo(j.get("corta").get("metros").asInt());
            assertThat(j.get("segura").get("exposicion").asDouble()).isLessThanOrEqualTo(j.get("corta").get("exposicion").asDouble());
        }
        mvc.perform(conToken(get("/api/rutas/segura"), c)
                        .param("origenLat", "4.60").param("origenLng", "-74.08")
                        .param("destinoLat", "10.4486").param("destinoLng", "-73.2464"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------------- HU 06 ----------------

    @Test
    @DisplayName("PF-17 Rutas frecuentes: guardar, editar, listar y borrar; nadie más las ve")
    void rutasFrecuentes() throws Exception {
        Cuenta c = cuenta("Frecuente");
        Cuenta otro = cuenta("Ajeno");
        String cuerpo = """
                {"nombre":"Casa al colegio","origenLatitud":10.4569,"origenLongitud":-73.2562,
                 "destinoLatitud":10.4486,"destinoLongitud":-73.2464,"modo":"A_PIE"}""";
        long id = leerId(mvc.perform(conToken(post("/api/rutas-frecuentes"), c)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated()));
        mvc.perform(conToken(put("/api/rutas-frecuentes/" + id), c).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("Casa al colegio", "Casa al gimnasio").replace("A_PIE", "BICICLETA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Casa al gimnasio"))
                .andExpect(jsonPath("$.modo").value("BICICLETA"));
        mvc.perform(conToken(get("/api/rutas-frecuentes"), c)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(conToken(delete("/api/rutas-frecuentes/" + id), otro)).andExpect(status().isNotFound());
        mvc.perform(conToken(delete("/api/rutas-frecuentes/" + id), c)).andExpect(status().isNoContent());
        mvc.perform(conToken(get("/api/rutas-frecuentes"), c)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PF-18 Seguimiento: máximo 1 hora, el guardián ve la posición y se puede detener")
    void seguimiento() throws Exception {
        Cuenta protegido = cuenta("Protegido");
        Cuenta guardian = cuenta("Guardian");
        agregarGuardian(protegido, guardian).andExpect(status().isCreated());

        mvc.perform(conToken(post("/api/seguimiento"), protegido).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutos\":90,\"latitud\":10.4569,\"longitud\":-73.2562}"))
                .andExpect(status().isBadRequest());
        String r = mvc.perform(conToken(post("/api/seguimiento"), protegido).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutos\":60,\"latitud\":10.4569,\"longitud\":-73.2562}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.guardianesAvisados").value(1))
                .andReturn().getResponse().getContentAsString();
        JsonNode s = json.readTree(r);
        long minutos = java.time.Duration.between(OffsetDateTime.parse(s.get("iniciadoEn").asText()),
                OffsetDateTime.parse(s.get("expiraEn").asText())).toMinutes();
        assertThat(minutos).isEqualTo(60);

        mvc.perform(conToken(put("/api/seguimiento/posicion"), protegido).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\":10.4530,\"longitud\":-73.2510}")).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/guardianes/protegidos"), guardian))
                .andExpect(jsonPath("$[0].estado").value("TRAYECTO"))
                .andExpect(jsonPath("$[0].latitud").value(10.4530));
        mvc.perform(conToken(get("/api/notificaciones"), guardian))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("SEGUIMIENTO_PROTEGIDO"));

        mvc.perform(conToken(post("/api/seguimiento/detener"), protegido)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/seguimiento"), protegido)).andExpect(status().isNoContent());
        mvc.perform(conToken(get("/api/guardianes/protegidos"), guardian))
                .andExpect(jsonPath("$[0].estado").value("TRANQUILO"))
                .andExpect(jsonPath("$[0].latitud").doesNotExist());
    }

    // ---------------- HU 12 ----------------

    @Test
    @DisplayName("PF-19 Guardianes: máximo 3, sin repetir, sin uno mismo y solo cuentas existentes")
    void limiteGuardianes() throws Exception {
        Cuenta yo = cuenta("Limite");
        for (int i = 0; i < 3; i++) {
            agregarGuardian(yo, cuenta("G" + i)).andExpect(status().isCreated());
        }
        Cuenta cuarto = cuenta("Cuarto");
        agregarGuardian(yo, cuarto).andExpect(status().isConflict());
        agregarGuardian(yo, yo).andExpect(status().isBadRequest());
        mvc.perform(conToken(post("/api/guardianes"), yo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@prueba.co\"}"))
                .andExpect(status().isNotFound());
        String lista = mvc.perform(conToken(get("/api/guardianes"), yo)).andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        long primero = json.readTree(lista).get(0).get("id").asLong();
        mvc.perform(conToken(delete("/api/guardianes/" + primero), yo)).andExpect(status().isNoContent());
        agregarGuardian(yo, cuarto).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PF-20 Pánico con guardianes: reciben la alerta con ubicación y la dejan de ver al cancelarla")
    void panicoAvisaGuardianes() throws Exception {
        Cuenta protegido = cuenta("Panico");
        Cuenta guardian = cuenta("Cuidador");
        Cuenta extrano = cuenta("Extrano");
        agregarGuardian(protegido, guardian).andExpect(status().isCreated());

        long alerta = leerId(mvc.perform(conToken(post("/api/alertas"), protegido).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\":10.4505,\"longitud\":-73.2487}")).andExpect(status().isCreated()));

        mvc.perform(conToken(get("/api/notificaciones"), guardian))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("PANICO_PROTEGIDO"))
                .andExpect(jsonPath("$.notificaciones[0].latitud").value(10.4505));
        mvc.perform(conToken(get("/api/guardianes/protegidos"), guardian))
                .andExpect(jsonPath("$[0].estado").value("PANICO"))
                .andExpect(jsonPath("$[0].alertaId").value(alerta));
        // La app manda la posición mientras dura la alerta y el guardián la ve moverse.
        mvc.perform(conToken(put("/api/seguimiento/posicion"), protegido).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\":10.4512,\"longitud\":-73.2480}")).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/guardianes/protegidos"), guardian)).andExpect(jsonPath("$[0].latitud").value(10.4512));
        // Quien no es guardián no ve a nadie.
        mvc.perform(conToken(get("/api/guardianes/protegidos"), extrano)).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(conToken(post("/api/alertas/" + alerta + "/cancelar"), protegido)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/guardianes/protegidos"), guardian)).andExpect(jsonPath("$[0].estado").value("TRANQUILO"));
    }

    // ---------------- HU 10 ----------------

    @Test
    @DisplayName("PF-21 2FA: activar con código válido, ingresar en dos pasos y desactivar")
    void dosFactores() throws Exception {
        Cuenta c = cuenta("DosPasos");
        String prep = mvc.perform(conToken(post("/api/auth/2fa/preparar"), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secreto = json.readTree(prep).get("secreto").asText();
        assertThat(json.readTree(prep).get("uri").asText()).startsWith("otpauth://totp/");

        mvc.perform(conToken(post("/api/auth/2fa/activar"), c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"000000\"}")).andExpect(status().isBadRequest());
        mvc.perform(conToken(post("/api/auth/2fa/activar"), c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + Totp.codigoActual(secreto) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosFactores").value(true));

        String paso1 = mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + c.correo() + "\",\"clave\":\"ClaveSegura1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereSegundoFactor").value(true))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String desafio = json.readTree(paso1).get("desafio").asText();
        // El desafío no sirve como sesión.
        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + desafio)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/ingreso/2fa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desafio\":\"" + desafio + "\",\"codigo\":\"" + Totp.codigoActual(secreto) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.dosFactores").value(true));

        mvc.perform(conToken(post("/api/auth/2fa/desactivar"), c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clave\":\"ClaveSegura1\",\"codigo\":\"" + Totp.codigoActual(secreto) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosFactores").value(false));
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + c.correo() + "\",\"clave\":\"ClaveSegura1\"}"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("PF-22 Cuenta: cambiar clave y correo exige la contraseña actual")
    void cambiosDeCuenta() throws Exception {
        Cuenta c = cuenta("Cambios");
        mvc.perform(conToken(put("/api/auth/yo/clave"), c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"Equivocada9\",\"nueva\":\"NuevaClave22\"}")).andExpect(status().isBadRequest());
        mvc.perform(conToken(put("/api/auth/yo/clave"), c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"ClaveSegura1\",\"nueva\":\"NuevaClave22\"}")).andExpect(status().isNoContent());
        String nuevo = "nuevo." + c.correo();
        mvc.perform(conToken(put("/api/auth/yo/correo"), c).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clave\":\"NuevaClave22\",\"correo\":\"" + nuevo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(nuevo));
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + nuevo + "\",\"clave\":\"NuevaClave22\"}"))
                .andExpect(status().isOk());
    }

    // ---------------- HU 01 (recuperación) ----------------

    @Test
    @DisplayName("PF-23 Recuperar clave: el código llega por mensaje, sirve una vez y no revela cuentas")
    void recuperarClave() throws Exception {
        Cuenta c = cuenta("Olvido");
        mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"no.existe@prueba.co\"}")).andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + c.correo() + "\"}")).andExpect(status().isAccepted());
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(BuzonPrueba.ULTIMO.get(c.correo()));
        assertThat(m.find()).isTrue();
        String codigo = m.group(1);
        String cuerpo = "{\"correo\":\"" + c.correo() + "\",\"codigo\":\"" + codigo + "\",\"nueva\":\"Recuperada33\"}";
        mvc.perform(post("/api/auth/recuperar/confirmar").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/recuperar/confirmar").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + c.correo() + "\",\"clave\":\"Recuperada33\"}")).andExpect(status().isOk());
    }

    // ---------------- HU 13 ----------------

    @Test
    @DisplayName("PF-24 Trazabilidad: el autor recibe una notificación cuando la autoridad cambia el estado")
    void avisoCambioEstado() throws Exception {
        Cuenta autora = cuenta("Trazable");
        long id = crearReporte(autora);
        usuarios.save(new Usuario("Teniente Dos", "teniente2." + UUID.randomUUID() + "@prueba.co",
                codificador.encode("ClaveSegura1"), null, Rol.AUTORIDAD, null));
        var autoridad = usuarios.findAll().stream().filter(u -> u.getRol() == Rol.AUTORIDAD).findFirst().orElseThrow();
        String r = mvc.perform(post("/api/auth/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + autoridad.getCorreo() + "\",\"clave\":\"ClaveSegura1\"}"))
                .andReturn().getResponse().getContentAsString();
        String tokenAutoridad = json.readTree(r).get("token").asText();
        mvc.perform(patch("/api/panel/reportes/" + id + "/estado").header("Authorization", "Bearer " + tokenAutoridad)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_REVISION\",\"nota\":\"Patrulla asignada\"}"))
                .andExpect(status().isOk());
        mvc.perform(conToken(get("/api/notificaciones"), autora))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("ESTADO_REPORTE"))
                .andExpect(jsonPath("$.notificaciones[0].cuerpo").value("Patrulla asignada"));
    }

    // ---------------- utilidades ----------------

    private Cuenta cuenta(String nombre) throws Exception {
        String correo = nombre.toLowerCase() + "." + UUID.randomUUID().toString().substring(0, 8) + "@prueba.co";
        String cuerpo = json.writeValueAsString(Map.of("nombre", nombre, "correo", correo, "clave", "ClaveSegura1",
                "comuna", 3, "aceptoPolitica", true));
        String r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode j = json.readTree(r);
        return new Cuenta(correo, j.get("token").asText(), j.get("usuario").get("id").asLong());
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder b, Cuenta c) {
        return b.header("Authorization", "Bearer " + c.token());
    }

    private void ubicar(Cuenta c, double lat, double lng) throws Exception {
        mvc.perform(conToken(put("/api/auth/yo/ubicacion"), c).contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\":" + lat + ",\"longitud\":" + lng + "}")).andExpect(status().isNoContent());
    }

    private ResultActions agregarGuardian(Cuenta protegido, Cuenta guardian) throws Exception {
        return mvc.perform(conToken(post("/api/guardianes"), protegido).contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + guardian.correo() + "\"}"));
    }

    private long crearReporte(Cuenta c) throws Exception {
        String fecha = OffsetDateTime.now(ZoneOffset.ofHours(-5)).minusHours(1).withNano(0).toString();
        String cuerpo = """
                {"tipoDelito":"ATRACO","arma":"ARMA_FUEGO","medio":"MOTOCICLETA","elementoHurtado":"CELULAR",
                 "descripcion":"Prueba de alerta","fechaHecho":"%s",
                 "latitud":10.4540,"longitud":-73.2529,"comuna":3,"barrio":"Primero de Mayo","anonimo":true}
                """.formatted(fecha);
        return leerId(mvc.perform(conToken(post("/api/reportes"), c).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()));
    }

    private long leerId(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
}
