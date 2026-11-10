package co.edu.unad.alertapp.alerta;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.guardian.GuardianServicio;
import co.edu.unad.alertapp.notificacion.Notificacion;
import co.edu.unad.alertapp.seguimiento.SeguimientoServicio;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * HU 11 - Botón de pánico. La alerta queda registrada con la ubicación GPS, aparece de inmediato en el panel
 * de la autoridad y llega en tiempo real a los guardianes del usuario (HU 12), que pueden seguir su posición
 * durante una hora. El envío por SMS queda para TRL 6.
 */
@Service
public class AlertaServicio {

    private final AlertaRepositorio alertas;
    private final UsuarioRepositorio usuarios;
    private final GuardianServicio guardianes;
    private final SeguimientoServicio seguimiento;

    public AlertaServicio(AlertaRepositorio alertas, UsuarioRepositorio usuarios, GuardianServicio guardianes,
                          SeguimientoServicio seguimiento) {
        this.alertas = alertas;
        this.usuarios = usuarios;
        this.guardianes = guardianes;
        this.seguimiento = seguimiento;
    }

    public record AlertaRespuesta(Long id, String usuario, String telefono, double latitud, double longitud,
                                  AlertaPanico.Estado estado, String nota, OffsetDateTime creadoEn,
                                  OffsetDateTime cerradaEn) {
        static AlertaRespuesta de(AlertaPanico a) {
            return new AlertaRespuesta(a.getId(), a.getUsuario().getNombre(), a.getUsuario().getTelefono(),
                    a.getLatitud(), a.getLongitud(), a.getEstado(), a.getNota(), a.getCreadoEn(), a.getCerradaEn());
        }
    }

    /** Si el usuario ya tiene una alerta activa se devuelve la misma para evitar duplicados por doble toque. */
    @Transactional
    public AlertaRespuesta activar(UsuarioAutenticado actual, double lat, double lng) {
        return alertas.findFirstByUsuarioIdAndEstado(actual.id(), AlertaPanico.Estado.ACTIVA)
                .map(AlertaRespuesta::de)
                .orElseGet(() -> {
                    var u = usuarios.findById(actual.id())
                            .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
                    AlertaPanico a = alertas.save(new AlertaPanico(u, lat, lng));
                    seguimiento.iniciarPorPanico(u, lat, lng);
                    guardianes.notificar(u.getId(), Notificacion.Tipo.PANICO_PROTEGIDO,
                            u.getNombre() + " activó el botón de pánico",
                            "Mira su ubicación en el mapa y llámalo. Si no contesta, llama al 123.", lat, lng);
                    return AlertaRespuesta.de(a);
                });
    }

    @Transactional
    public AlertaRespuesta cancelar(UsuarioAutenticado actual, Long id) {
        AlertaPanico a = buscar(id);
        if (!a.getUsuario().getId().equals(actual.id())) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "Solo puedes cancelar tus alertas");
        }
        if (a.getEstado() != AlertaPanico.Estado.ACTIVA) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "La alerta ya no está activa");
        }
        a.cerrar(AlertaPanico.Estado.CANCELADA, "Cancelada por el usuario");
        seguimiento.detenerPorPanico(actual.id());
        return AlertaRespuesta.de(a);
    }

    @Transactional
    public AlertaRespuesta atender(Long id, String nota) {
        AlertaPanico a = buscar(id);
        if (a.getEstado() != AlertaPanico.Estado.ACTIVA) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "La alerta ya fue cerrada");
        }
        a.cerrar(AlertaPanico.Estado.ATENDIDA, nota == null || nota.isBlank() ? "Atendida" : nota.trim());
        seguimiento.detenerPorPanico(a.getUsuario().getId());
        return AlertaRespuesta.de(a);
    }

    @Transactional(readOnly = true)
    public List<AlertaRespuesta> mias(UsuarioAutenticado actual) {
        return alertas.findTop20ByUsuarioIdOrderByCreadoEnDesc(actual.id()).stream().map(AlertaRespuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaRespuesta> recientes() {
        return alertas.findTop50ByOrderByCreadoEnDesc().stream().map(AlertaRespuesta::de).toList();
    }

    private AlertaPanico buscar(Long id) {
        return alertas.findById(id).orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "La alerta no existe"));
    }
}
