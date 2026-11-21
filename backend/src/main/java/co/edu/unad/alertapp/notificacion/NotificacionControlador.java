package co.edu.unad.alertapp.notificacion;

import co.edu.unad.alertapp.notificacion.NotificacionServicio.Bandeja;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** HU 02 - Recepción de alertas geolocalizadas. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionControlador {

    private final NotificacionServicio servicio;
    private final CanalTiempoReal canal;

    public NotificacionControlador(NotificacionServicio servicio, CanalTiempoReal canal) {
        this.servicio = servicio;
        this.canal = canal;
    }

    /** Conexión SSE. Eventos: "notificacion" (personal) y "reporte" (reporte nuevo en el mapa). */
    @GetMapping(value = "/flujo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flujo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return canal.conectar(usuario.id());
    }

    @GetMapping
    public Bandeja bandeja(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.bandeja(usuario.id());
    }

    @PostMapping("/{id}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leida(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicio.marcarLeida(usuario.id(), id);
    }

    @PostMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void todas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicio.marcarTodas(usuario.id());
    }
}
