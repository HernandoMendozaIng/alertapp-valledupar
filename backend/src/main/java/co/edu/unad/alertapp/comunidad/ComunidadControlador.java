package co.edu.unad.alertapp.comunidad;

import co.edu.unad.alertapp.comunidad.ComunidadServicio.ComentarioRespuesta;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReporteRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU 07 - Validación comunitaria y comentarios; HU 13 - trazabilidad del caso. */
@RestController
@RequestMapping("/api")
public class ComunidadControlador {

    private final ComunidadServicio servicio;

    public ComunidadControlador(ComunidadServicio servicio) {
        this.servicio = servicio;
    }

    public record VotoPeticion(@NotNull(message = "Indica si confirmas o si es falso") Voto.Tipo tipo) {}

    public record ComentarioPeticion(
            @NotBlank(message = "Escribe un comentario") @Size(max = 400, message = "Máximo 400 caracteres") String texto) {}

    @PutMapping("/reportes/{id}/voto")
    public ReporteRespuesta votar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                  @Valid @RequestBody VotoPeticion peticion) {
        return servicio.votar(usuario, id, peticion.tipo());
    }

    @DeleteMapping("/reportes/{id}/voto")
    public ReporteRespuesta quitarVoto(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servicio.quitarVoto(usuario, id);
    }

    @GetMapping("/reportes/{id}/comentarios")
    public List<ComentarioRespuesta> comentarios(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @PathVariable Long id) {
        return servicio.comentarios(usuario, id);
    }

    @PostMapping("/reportes/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioRespuesta comentar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                        @Valid @RequestBody ComentarioPeticion peticion) {
        return servicio.comentar(usuario, id, peticion.texto());
    }

    @DeleteMapping("/comentarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarComentario(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicio.borrarComentario(usuario, id);
    }
}
