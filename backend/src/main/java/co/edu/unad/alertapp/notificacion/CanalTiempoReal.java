package co.edu.unad.alertapp.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HU 02 - Canal de eventos en tiempo real con Server-Sent Events (SSE).
 *
 * Cada usuario con la app abierta mantiene una conexión HTTP abierta; el servidor escribe los eventos
 * en cuanto ocurren, sin que el cliente tenga que preguntar. Es el equivalente en web de una
 * notificación push: en la arquitectura objetivo se reemplaza por Firebase Cloud Messaging o Amazon SNS
 * para llegar también con la app cerrada.
 *
 * Las conexiones se guardan en memoria, lo que basta para una sola instancia del prototipo.
 */
@Component
public class CanalTiempoReal {

    private static final Logger LOG = LoggerFactory.getLogger(CanalTiempoReal.class);
    private static final long DURACION_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, List<SseEmitter>> conexiones = new ConcurrentHashMap<>();

    public SseEmitter conectar(Long usuarioId) {
        SseEmitter emisor = new SseEmitter(DURACION_MS);
        conexiones.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>()).add(emisor);
        Runnable quitar = () -> quitar(usuarioId, emisor);
        emisor.onCompletion(quitar);
        emisor.onTimeout(quitar);
        emisor.onError(e -> quitar.run());
        enviar(usuarioId, emisor, "conectado", Map.of("ok", true));
        return emisor;
    }

    /** Envía un evento a todas las pestañas abiertas de un usuario. */
    public void aUsuario(Long usuarioId, String evento, Object datos) {
        conexiones.getOrDefault(usuarioId, List.of()).forEach(e -> enviar(usuarioId, e, evento, datos));
    }

    /** Envía un evento a todos los usuarios conectados (por ejemplo, un reporte nuevo para refrescar el mapa). */
    public void aTodos(String evento, Object datos) {
        conexiones.forEach((id, lista) -> lista.forEach(e -> enviar(id, e, evento, datos)));
    }

    public int conectados() {
        return conexiones.values().stream().mapToInt(List::size).sum();
    }

    public boolean estaConectado(Long usuarioId) {
        return !conexiones.getOrDefault(usuarioId, List.of()).isEmpty();
    }

    /** Comentario periódico para que proxies y navegadores no cierren la conexión por inactividad. */
    @Scheduled(fixedRate = 25_000)
    void latido() {
        conexiones.forEach((id, lista) -> lista.forEach(e -> {
            try {
                e.send(SseEmitter.event().comment("latido"));
            } catch (IOException | IllegalStateException ex) {
                quitar(id, e);
            }
        }));
    }

    private void enviar(Long usuarioId, SseEmitter emisor, String evento, Object datos) {
        try {
            emisor.send(SseEmitter.event().name(evento).data(datos));
        } catch (IOException | IllegalStateException e) {
            LOG.debug("Conexión SSE cerrada para el usuario {}", usuarioId);
            quitar(usuarioId, emisor);
        }
    }

    private void quitar(Long usuarioId, SseEmitter emisor) {
        conexiones.computeIfPresent(usuarioId, (k, lista) -> {
            lista.remove(emisor);
            return lista.isEmpty() ? null : lista;
        });
    }
}
