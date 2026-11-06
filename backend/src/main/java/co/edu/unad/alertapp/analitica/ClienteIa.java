package co.edu.unad.alertapp.analitica;

import co.edu.unad.alertapp.analitica.AnaliticaDtos.PuntoIa;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.RespuestaHotspots;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.Riesgo;
import co.edu.unad.alertapp.analitica.AnaliticaDtos.SolicitudHotspots;
import co.edu.unad.alertapp.config.AlertAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Cliente HTTP del microservicio de IA (Python + scikit-learn). Si falla, devuelve vacío. */
@Component
public class ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(ClienteIa.class);

    private final RestClient cliente;

    public ClienteIa(AlertAppProperties props) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(props.ia().timeoutMs());
        fabrica.setReadTimeout(props.ia().timeoutMs());
        this.cliente = RestClient.builder().baseUrl(props.ia().url()).requestFactory(fabrica).build();
    }

    public record SolicitudRiesgo(List<PuntoIa> puntos, double lat, double lng, OffsetDateTime momento) {}

    public Optional<RespuestaHotspots> hotspots(SolicitudHotspots solicitud) {
        try {
            return Optional.ofNullable(cliente.post().uri("/hotspots")
                    .contentType(MediaType.APPLICATION_JSON).body(solicitud)
                    .retrieve().body(RespuestaHotspots.class));
        } catch (Exception e) {
            log.warn("Servicio de IA no disponible para hotspots: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Riesgo> riesgo(SolicitudRiesgo solicitud) {
        try {
            return Optional.ofNullable(cliente.post().uri("/riesgo")
                    .contentType(MediaType.APPLICATION_JSON).body(solicitud)
                    .retrieve().body(Riesgo.class));
        } catch (Exception e) {
            log.warn("Servicio de IA no disponible para riesgo: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
