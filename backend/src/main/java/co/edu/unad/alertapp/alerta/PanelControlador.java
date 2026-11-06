package co.edu.unad.alertapp.alerta;

import co.edu.unad.alertapp.alerta.AlertaServicio.AlertaRespuesta;
import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.comunidad.ComunidadServicio;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReporteRespuesta;
import co.edu.unad.alertapp.reporte.ReporteServicio;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Panel web de la autoridad (Policía / Alcaldía). Solo accesible con rol AUTORIDAD.
 * Permite gestionar el estado de los reportes (HU 13) y atender las alertas de pánico (HU 11).
 */
@RestController
@RequestMapping("/api/panel")
public class PanelControlador {

    /** Transiciones que puede hacer la autoridad desde cada estado. */
    private static final Map<Estado, Set<Estado>> TRANSICIONES = Map.of(
            Estado.PENDIENTE, Set.of(Estado.EN_REVISION, Estado.DESCARTADO),
            Estado.VALIDADO, Set.of(Estado.EN_REVISION, Estado.DESCARTADO),
            Estado.EN_REVISION, Set.of(Estado.CERRADO, Estado.DESCARTADO),
            Estado.DESCARTADO, Set.of(Estado.EN_REVISION),
            Estado.CERRADO, Set.of(Estado.EN_REVISION));

    private final ReporteServicio reportes;
    private final ComunidadServicio comunidad;
    private final AlertaServicio alertas;

    public PanelControlador(ReporteServicio reportes, ComunidadServicio comunidad, AlertaServicio alertas) {
        this.reportes = reportes;
        this.comunidad = comunidad;
        this.alertas = alertas;
    }

    public record CambioEstado(@NotNull(message = "Indica el nuevo estado") Estado estado,
                               @Size(max = 400) String nota) {}

    public record Atencion(@Size(max = 200) String nota) {}

    @PatchMapping("/reportes/{id}/estado")
    @Transactional
    public ReporteRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                          @Valid @RequestBody CambioEstado cambio) {
        Reporte r = reportes.buscar(id);
        if (!TRANSICIONES.getOrDefault(r.getEstado(), Set.of()).contains(cambio.estado())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT,
                    "No se puede pasar de " + r.getEstado() + " a " + cambio.estado());
        }
        r.setEstado(cambio.estado());
        if (cambio.nota() != null && !cambio.nota().isBlank()) {
            comunidad.comentar(usuario, id, cambio.nota());
        }
        return ReporteRespuesta.de(r, usuario.id(), null);
    }

    @GetMapping("/alertas")
    public List<AlertaRespuesta> alertas() {
        return alertas.recientes();
    }

    @PostMapping("/alertas/{id}/atender")
    public AlertaRespuesta atender(@PathVariable Long id, @Valid @RequestBody(required = false) Atencion atencion) {
        return alertas.atender(id, atencion == null ? null : atencion.nota());
    }
}
