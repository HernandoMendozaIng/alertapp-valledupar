package co.edu.unad.alertapp.seguimiento;

import co.edu.unad.alertapp.seguimiento.SeguimientoServicio.RutaPeticion;
import co.edu.unad.alertapp.seguimiento.SeguimientoServicio.RutaRespuesta;
import co.edu.unad.alertapp.seguimiento.SeguimientoServicio.SeguimientoRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU 06 - Gestión de seguridad y rutas frecuentes. */
@RestController
public class SeguimientoControlador {

    private final SeguimientoServicio servicio;

    public SeguimientoControlador(SeguimientoServicio servicio) {
        this.servicio = servicio;
    }

    public record RutaFormulario(
            @NotBlank(message = "Ponle un nombre a la ruta") @Size(max = 60, message = "Máximo 60 letras") String nombre,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double origenLatitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double origenLongitud,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double destinoLatitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double destinoLongitud,
            @NotNull(message = "Elige cómo te desplazas") ModoTransporte modo) {

        RutaPeticion peticion() {
            return new RutaPeticion(nombre, origenLatitud, origenLongitud, destinoLatitud, destinoLongitud, modo);
        }
    }

    public record InicioSeguimiento(
            @Min(value = 5, message = "Mínimo 5 minutos") @Max(value = 60, message = "Máximo 1 hora") int minutos,
            Long rutaId,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {}

    public record Posicion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {}

    @GetMapping("/api/rutas-frecuentes")
    public List<RutaRespuesta> rutas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.rutas(usuario.id());
    }

    @PostMapping("/api/rutas-frecuentes")
    @ResponseStatus(HttpStatus.CREATED)
    public RutaRespuesta guardar(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody RutaFormulario f) {
        return servicio.guardarRuta(usuario.id(), f.peticion());
    }

    @PutMapping("/api/rutas-frecuentes/{id}")
    public RutaRespuesta editar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                @Valid @RequestBody RutaFormulario f) {
        return servicio.editarRuta(usuario.id(), id, f.peticion());
    }

    @DeleteMapping("/api/rutas-frecuentes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicio.borrarRuta(usuario.id(), id);
    }

    @GetMapping("/api/seguimiento")
    public ResponseEntity<SeguimientoRespuesta> actual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.actual(usuario.id()).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/api/seguimiento")
    @ResponseStatus(HttpStatus.CREATED)
    public SeguimientoRespuesta iniciar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                        @Valid @RequestBody InicioSeguimiento p) {
        return servicio.iniciar(usuario.id(), p.minutos(), p.rutaId(), p.latitud(), p.longitud());
    }

    @PutMapping("/api/seguimiento/posicion")
    public SeguimientoRespuesta mover(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody Posicion p) {
        return servicio.mover(usuario.id(), p.latitud(), p.longitud());
    }

    @PostMapping("/api/seguimiento/detener")
    public ResponseEntity<SeguimientoRespuesta> detener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.detener(usuario.id()).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
}
