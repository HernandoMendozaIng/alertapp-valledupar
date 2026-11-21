package co.edu.unad.alertapp.guardian;

import co.edu.unad.alertapp.guardian.GuardianServicio.GuardianRespuesta;
import co.edu.unad.alertapp.guardian.GuardianServicio.ProtegidoRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU 12 - Configuración de la red de apoyo (guardianes). */
@RestController
@RequestMapping("/api/guardianes")
public class GuardianControlador {

    private final GuardianServicio servicio;

    public GuardianControlador(GuardianServicio servicio) {
        this.servicio = servicio;
    }

    public record NuevoGuardian(@NotBlank(message = "Escribe el correo de tu guardián")
                                @Email(message = "El correo no es válido") String correo) {}

    @GetMapping
    public List<GuardianRespuesta> mios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.mios(usuario.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianRespuesta agregar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @Valid @RequestBody NuevoGuardian peticion) {
        return servicio.agregar(usuario.id(), peticion.correo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicio.quitar(usuario.id(), id);
    }

    /** Personas que me eligieron como guardián, con su estado y su ubicación si están en alerta o en trayecto. */
    @GetMapping("/protegidos")
    public List<ProtegidoRespuesta> protegidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.protegidos(usuario.id());
    }
}
