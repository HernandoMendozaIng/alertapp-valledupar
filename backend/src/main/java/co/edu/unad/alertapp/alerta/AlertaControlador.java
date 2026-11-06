package co.edu.unad.alertapp.alerta;

import co.edu.unad.alertapp.alerta.AlertaServicio.AlertaRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU 11 - Activación de alerta crítica (botón de pánico). */
@RestController
@RequestMapping("/api/alertas")
public class AlertaControlador {

    private final AlertaServicio servicio;

    public AlertaControlador(AlertaServicio servicio) {
        this.servicio = servicio;
    }

    public record AlertaPeticion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertaRespuesta activar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                   @Valid @RequestBody AlertaPeticion peticion) {
        return servicio.activar(usuario, peticion.latitud(), peticion.longitud());
    }

    @PostMapping("/{id}/cancelar")
    public AlertaRespuesta cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servicio.cancelar(usuario, id);
    }

    @GetMapping("/mias")
    public List<AlertaRespuesta> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.mias(usuario);
    }
}
