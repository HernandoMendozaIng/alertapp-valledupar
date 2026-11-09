package co.edu.unad.alertapp.usuario;

import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import co.edu.unad.alertapp.usuario.AuthDtos.CambioClave;
import co.edu.unad.alertapp.usuario.AuthDtos.CambioCorreo;
import co.edu.unad.alertapp.usuario.AuthDtos.CodigoPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.ConfiguracionDosFactores;
import co.edu.unad.alertapp.usuario.AuthDtos.DesactivarDosFactores;
import co.edu.unad.alertapp.usuario.AuthDtos.IngresoPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.PerfilActualizacion;
import co.edu.unad.alertapp.usuario.AuthDtos.Preferencias;
import co.edu.unad.alertapp.usuario.AuthDtos.RecuperarPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.RegistroPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.RestablecerPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.SegundoFactorPeticion;
import co.edu.unad.alertapp.usuario.AuthDtos.SesionRespuesta;
import co.edu.unad.alertapp.usuario.AuthDtos.UbicacionPeticion;
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

/** HU 01 - Acceso y autenticación. HU 10 - Configuración y seguridad de la cuenta (2FA). */
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

    @PostMapping("/ingreso/2fa")
    public SesionRespuesta segundoFactor(@Valid @RequestBody SegundoFactorPeticion peticion) {
        return servicio.segundoFactor(peticion.desafio(), peticion.codigo());
    }

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recuperar(@Valid @RequestBody RecuperarPeticion peticion) {
        servicio.solicitarRecuperacion(peticion.correo());
    }

    @PostMapping("/recuperar/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerPeticion peticion) {
        servicio.restablecer(peticion);
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

    @PutMapping("/yo/preferencias")
    public UsuarioRespuesta preferencias(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @RequestBody Preferencias peticion) {
        return servicio.preferencias(usuario.id(), peticion);
    }

    /** La app informa la ubicación al abrir el mapa para saber a quién alertar (HU 02). */
    @PutMapping("/yo/ubicacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ubicacion(@AuthenticationPrincipal UsuarioAutenticado usuario,
                          @Valid @RequestBody UbicacionPeticion peticion) {
        servicio.ubicar(usuario.id(), peticion.latitud(), peticion.longitud());
    }

    @PutMapping("/yo/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clave(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CambioClave peticion) {
        servicio.cambiarClave(usuario.id(), peticion);
    }

    @PutMapping("/yo/correo")
    public UsuarioRespuesta correo(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                   @Valid @RequestBody CambioCorreo peticion) {
        return servicio.cambiarCorreo(usuario.id(), peticion);
    }

    @PostMapping("/2fa/preparar")
    public ConfiguracionDosFactores prepararDosFactores(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.prepararDosFactores(usuario.id());
    }

    @PostMapping("/2fa/activar")
    public UsuarioRespuesta activarDosFactores(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                               @Valid @RequestBody CodigoPeticion peticion) {
        return servicio.activarDosFactores(usuario.id(), peticion.codigo());
    }

    @PostMapping("/2fa/desactivar")
    public UsuarioRespuesta desactivarDosFactores(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Valid @RequestBody DesactivarDosFactores peticion) {
        return servicio.desactivarDosFactores(usuario.id(), peticion.clave(), peticion.codigo());
    }
}
