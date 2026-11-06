package co.edu.unad.alertapp.config;

import co.edu.unad.alertapp.comunidad.Voto;
import co.edu.unad.alertapp.comunidad.VotoRepositorio;
import co.edu.unad.alertapp.reporte.Catalogos;
import co.edu.unad.alertapp.reporte.ClasificadorSeveridad;
import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.reporte.ReporteRepositorio;
import co.edu.unad.alertapp.usuario.Rol;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Carga datos SINTÉTICOS de prueba la primera vez que arranca el prototipo (base de datos vacía):
 * cuentas de demostración y 154 reportes generados dentro de los límites de las Comunas 3 y 4.
 * No son denuncias reales; sirven para las pruebas funcionales del nivel TRL 5.
 * Las fechas se desplazan por semanas completas para que los datos queden en los últimos 90 días.
 */
@Component
public class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);

    private final AlertAppProperties props;
    private final UsuarioRepositorio usuarios;
    private final ReporteRepositorio reportes;
    private final VotoRepositorio votos;
    private final PasswordEncoder codificador;
    private final ClasificadorSeveridad clasificador;
    private final ObjectMapper mapper;

    public DatosDemo(AlertAppProperties props, UsuarioRepositorio usuarios, ReporteRepositorio reportes,
                     VotoRepositorio votos, PasswordEncoder codificador, ClasificadorSeveridad clasificador, ObjectMapper mapper) {
        this.props = props;
        this.usuarios = usuarios;
        this.reportes = reportes;
        this.votos = votos;
        this.codificador = codificador;
        this.clasificador = clasificador;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!props.seed().habilitado() || usuarios.count() > 0) {
            return;
        }
        String clave = codificador.encode("Valledupar2026");
        usuarios.save(new Usuario("Teniente Rosa Daza", "autoridad@alertapp.co", clave, "3001234567", Rol.AUTORIDAD, null));
        Usuario demo = usuarios.save(new Usuario("Luis Ariza", "ciudadano@alertapp.co", clave, "3015550142", Rol.CIUDADANO, (short) 3));
        List<Usuario> vecinos = new ArrayList<>();
        String[] nombres = {"Marta Oñate", "Jairo Mestre", "Yeimy Cuello", "Rafael Quintero", "Diana Maestre", "Óscar Fuentes"};
        for (int i = 0; i < nombres.length; i++) {
            vecinos.add(usuarios.save(new Usuario(nombres[i], "vecino" + (i + 1) + "@alertapp.co", clave, null, Rol.CIUDADANO,
                    (short) (i % 2 == 0 ? 3 : 4))));
        }

        JsonNode raiz;
        try (InputStream in = new ClassPathResource("seed/reportes-demo.json").getInputStream()) {
            raiz = mapper.readTree(in);
        }
        JsonNode lista = raiz.get("reportes");
        OffsetDateTime maxima = null;
        for (JsonNode n : lista) {
            OffsetDateTime f = OffsetDateTime.parse(n.get("fechaHecho").asText());
            if (maxima == null || f.isAfter(maxima)) maxima = f;
        }
        // Se corre por semanas completas para conservar el día de la semana y la hora de cada hecho.
        long desfaseSemanas = java.time.Duration.between(maxima, OffsetDateTime.now().minusDays(1)).toDays() / 7;

        Random azar = new Random(42);
        int i = 0;
        for (JsonNode n : lista) {
            Usuario autor = i % 9 == 0 ? demo : vecinos.get(azar.nextInt(vecinos.size()));
            Reporte r = new Reporte(autor);
            var tipo = Catalogos.TipoDelito.valueOf(n.get("tipoDelito").asText());
            var arma = Catalogos.Arma.valueOf(n.get("arma").asText());
            var medio = Catalogos.Medio.valueOf(n.get("medio").asText());
            var elemento = Catalogos.Elemento.valueOf(n.get("elementoHurtado").asText());
            r.setTipoDelito(tipo);
            r.setArma(arma);
            r.setMedio(medio);
            r.setElementoHurtado(elemento);
            r.setDescripcion(n.get("descripcion").asText());
            OffsetDateTime fecha = OffsetDateTime.parse(n.get("fechaHecho").asText()).plusWeeks(desfaseSemanas);
            r.setFechaHecho(fecha);
            r.setCreadoEn(fecha.plusMinutes(20 + azar.nextInt(200)));
            r.setLatitud(n.get("latitud").asDouble());
            r.setLongitud(n.get("longitud").asDouble());
            r.setComuna((short) n.get("comuna").asInt());
            r.setBarrio(n.hasNonNull("barrio") ? n.get("barrio").asText() : null);
            r.setAnonimo(azar.nextDouble() < 0.7);
            var res = clasificador.clasificar(tipo, arma, medio, elemento);
            r.setSeveridad(res.severidad(), res.puntaje());
            reportes.save(r);
            // Votos reales de otros vecinos (nunca del autor), para que la validación comunitaria tenga historia.
            List<Usuario> votantes = new ArrayList<>(vecinos);
            votantes.add(demo);
            votantes.removeIf(v -> v.getId().equals(autor.getId()));
            Collections.shuffle(votantes, azar);
            boolean falsoMasivo = azar.nextDouble() < 0.12;
            int confirma = falsoMasivo ? azar.nextInt(2) : azar.nextInt(Math.min(6, votantes.size()));
            int falso = falsoMasivo ? Math.min(3 + azar.nextInt(2), votantes.size() - confirma) : azar.nextInt(2);
            falso = Math.max(0, Math.min(falso, votantes.size() - confirma));
            for (int k = 0; k < confirma + falso; k++) {
                votos.save(new Voto(r, votantes.get(k), k < confirma ? Voto.Tipo.CONFIRMA : Voto.Tipo.FALSO));
            }
            r.setVotos(confirma, falso);
            int diferencia = confirma - falso;
            if (diferencia <= -props.validacion().umbral()) r.setEstado(Catalogos.Estado.DESCARTADO);
            else if (diferencia >= props.validacion().umbral()) r.setEstado(azar.nextDouble() < 0.3 ? Catalogos.Estado.EN_REVISION : Catalogos.Estado.VALIDADO);
            i++;
        }
        log.info("Datos de demostración cargados: {} usuarios y {} reportes sintéticos", usuarios.count(), reportes.count());
    }
}
