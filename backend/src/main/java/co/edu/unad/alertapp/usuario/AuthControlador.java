package co.edu.unad.alertapp.usuario;

import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import co.edu.unad.alertapp.usuario.AuthDtos.IngresoPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.PerfilActualizacion;
import co.edu.unad.alertapp.usuario.AuthDtos.RegistroPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.SesionRespuesta;
import co.edu.unad.alertapp.usuario.AuthDtos.UsuarioRespuesta;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU 01 - Acceso y autenticación de usuario. */
@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private final AuthServicio servicio;

    public AuthControlador(AuthServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionRespuesta registro(@Valid @RequestBody RegistroPeticion peticion) {
        return servicio.registrar(peticion);
    }

    @PostMapping("/ingreso")
    public SesionRespuesta ingreso(@Valid @RequestBody IngresoPeticion peticion) {
        return servicio.ingresar(peticion);
    }

    @GetMapping("/yo")
    public UsuarioRespuesta yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.perfil(usuario.id());
    }

    @PutMapping("/yo")
    public UsuarioRespuesta actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @Valid @RequestBody PerfilActualizacion peticion) {
        return servicio.actualizar(usuario.id(), peticion);
    }
}
