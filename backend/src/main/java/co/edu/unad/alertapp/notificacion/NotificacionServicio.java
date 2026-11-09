package co.edu.unad.alertapp.notificacion;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Guarda las notificaciones en la bandeja del usuario y las empuja por el canal en tiempo real.
 * El envío ocurre después del commit para no avisar de algo que luego se deshace.
 */
@Service
public class NotificacionServicio {

    private final NotificacionRepositorio notificaciones;
    private final CanalTiempoReal canal;

    public NotificacionServicio(NotificacionRepositorio notificaciones, CanalTiempoReal canal) {
        this.notificaciones = notificaciones;
        this.canal = canal;
    }

    public record NotificacionRespuesta(Long id, Notificacion.Tipo tipo, String titulo, String cuerpo, String enlace,
                                        Double latitud, Double longitud, boolean leida, OffsetDateTime creadoEn) {
        static NotificacionRespuesta de(Notificacion n) {
            return new NotificacionRespuesta(n.getId(), n.getTipo(), n.getTitulo(), n.getCuerpo(), n.getEnlace(),
                    n.getLatitud(), n.getLongitud(), n.isLeida(), n.getCreadoEn());
        }
    }

    public record Bandeja(long sinLeer, List<NotificacionRespuesta> notificaciones) {}

    @Transactional
    public void notificar(Usuario destino, Notificacion.Tipo tipo, String titulo, String cuerpo, String enlace,
                          Double lat, Double lng) {
        Notificacion n = notificaciones.save(new Notificacion(destino, tipo, titulo, cuerpo, enlace, lat, lng));
        NotificacionRespuesta dto = NotificacionRespuesta.de(n);
        Long id = destino.getId();
        despuesDelCommit(() -> canal.aUsuario(id, "notificacion", dto));
    }

    /** Evento sin bandeja que reciben todos los conectados. */
    public void difundir(String evento, Object datos) {
        despuesDelCommit(() -> canal.aTodos(evento, datos));
    }

    @Transactional(readOnly = true)
    public Bandeja bandeja(Long usuarioId) {
        return new Bandeja(notificaciones.countByUsuarioIdAndLeidaFalse(usuarioId),
                notificaciones.findTop30ByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream()
                        .map(NotificacionRespuesta::de).toList());
    }

    @Transactional
    public void marcarLeida(Long usuarioId, Long id) {
        Notificacion n = notificaciones.findById(id)
                .filter(x -> x.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "La notificación no existe"));
        n.marcarLeida();
    }

    @Transactional
    public void marcarTodas(Long usuarioId) {
        notificaciones.marcarTodasLeidas(usuarioId);
    }

    private static void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
