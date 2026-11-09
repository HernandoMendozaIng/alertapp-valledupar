package co.edu.unad.alertapp.guardian;

import co.edu.unad.alertapp.alerta.AlertaPanico;
import co.edu.unad.alertapp.alerta.AlertaRepositorio;
import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.notificacion.Notificacion;
import co.edu.unad.alertapp.notificacion.NotificacionServicio;
import co.edu.unad.alertapp.seguimiento.Seguimiento;
import co.edu.unad.alertapp.seguimiento.SeguimientoRepositorio;
import co.edu.unad.alertapp.usuario.Rol;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * HU 12 - Red de apoyo. Cada usuario designa hasta 3 guardianes entre las personas registradas. Los guardianes
 * reciben una notificación al instante cuando su protegido activa el botón de pánico o comparte su trayecto,
 * y ven su ubicación en el mapa mientras dure la alerta o el seguimiento. Fuera de eso no ven nada.
 */
@Service
public class GuardianServicio {

    public static final int MAXIMO = 3;

    private final GuardianRepositorio guardianes;
    private final UsuarioRepositorio usuarios;
    private final AlertaRepositorio alertas;
    private final SeguimientoRepositorio seguimientos;
    private final NotificacionServicio notificaciones;

    public GuardianServicio(GuardianRepositorio guardianes, UsuarioRepositorio usuarios, AlertaRepositorio alertas,
                            SeguimientoRepositorio seguimientos, NotificacionServicio notificaciones) {
        this.guardianes = guardianes;
        this.usuarios = usuarios;
        this.alertas = alertas;
        this.seguimientos = seguimientos;
        this.notificaciones = notificaciones;
    }

    public record GuardianRespuesta(Long id, String nombre, String correo, String telefono, OffsetDateTime desde) {
        static GuardianRespuesta de(Guardian g) {
            Usuario u = g.getGuardian();
            return new GuardianRespuesta(g.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), g.getCreadoEn());
        }
    }

    /**
     * Lo que ve el guardián de cada protegido. La ubicación solo viaja si hay una alerta o un seguimiento activo.
     */
    public record ProtegidoRespuesta(Long usuarioId, String nombre, String telefono, String estado,
                                     Long alertaId, Double latitud, Double longitud,
                                     OffsetDateTime ubicacionEn, OffsetDateTime expiraEn) {}

    @Transactional
    public GuardianRespuesta agregar(Long protegidoId, String correo) {
        Usuario protegido = usuario(protegidoId);
        Usuario g = usuarios.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND,
                        "No hay ninguna cuenta con ese correo. Pídele que se registre primero en AlertApp."));
        if (g.getId().equals(protegidoId)) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "No puedes ser tu propio guardián");
        }
        if (g.getRol() != Rol.CIUDADANO) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "Las cuentas de la autoridad no pueden ser guardianes");
        }
        if (guardianes.existsByProtegidoIdAndGuardianId(protegidoId, g.getId())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, g.getNombre() + " ya es tu guardián");
        }
        if (guardianes.countByProtegidoId(protegidoId) >= MAXIMO) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Ya tienes " + MAXIMO + " guardianes. Quita uno para agregar otro.");
        }
        Guardian nuevo = guardianes.save(new Guardian(protegido, g));
        notificaciones.notificar(g, Notificacion.Tipo.GUARDIAN_AGREGADO,
                protegido.getNombre() + " te eligió como guardián",
                "Te avisaremos si activa el botón de pánico o comparte su trayecto, y podrás ver dónde está mientras dure.",
                "/red", null, null);
        return GuardianRespuesta.de(nuevo);
    }

    @Transactional(readOnly = true)
    public List<GuardianRespuesta> mios(Long protegidoId) {
        return guardianes.findByProtegidoIdOrderByCreadoEn(protegidoId).stream().map(GuardianRespuesta::de).toList();
    }

    @Transactional
    public void quitar(Long protegidoId, Long id) {
        Guardian g = guardianes.findById(id)
                .filter(x -> x.getProtegido().getId().equals(protegidoId))
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "Ese guardián no está en tu red"));
        guardianes.delete(g);
    }

    @Transactional(readOnly = true)
    public List<ProtegidoRespuesta> protegidos(Long guardianId) {
        return guardianes.findByGuardianIdOrderByCreadoEn(guardianId).stream()
                .map(g -> estado(g.getProtegido()))
                .toList();
    }

    /** Avisa a todos los guardianes de un usuario (notificación guardada + evento en tiempo real). */
    @Transactional
    public int notificar(Long protegidoId, Notificacion.Tipo tipo, String titulo, String cuerpo, Double lat, Double lng) {
        var lista = guardianes.findByProtegidoIdOrderByCreadoEn(protegidoId);
        lista.forEach(g -> notificaciones.notificar(g.getGuardian(), tipo, titulo, cuerpo, "/red", lat, lng));
        return lista.size();
    }

    /** Empuja la nueva posición a los guardianes conectados para que su mapa se mueva sin recargar. */
    @Transactional(readOnly = true)
    public void difundirPosicion(Long protegidoId, double lat, double lng) {
        var datos = Map.of("usuarioId", protegidoId, "latitud", lat, "longitud", lng,
                "ubicacionEn", OffsetDateTime.now().toString());
        guardianes.findByProtegidoIdOrderByCreadoEn(protegidoId)
                .forEach(g -> notificaciones.enviar(g.getGuardian().getId(), "ubicacion", datos));
    }

    /** Avisa a los guardianes que el estado del protegido cambió (alerta cancelada o atendida, trayecto detenido). */
    @Transactional(readOnly = true)
    public void difundirEstado(Long protegidoId) {
        guardianes.findByProtegidoIdOrderByCreadoEn(protegidoId)
                .forEach(g -> notificaciones.enviar(g.getGuardian().getId(), "protegido", Map.of("usuarioId", protegidoId)));
    }

    private ProtegidoRespuesta estado(Usuario p) {
        var alerta = alertas.findFirstByUsuarioIdAndEstado(p.getId(), AlertaPanico.Estado.ACTIVA);
        var seguimiento = seguimientos.activo(p.getId());
        if (alerta.isPresent()) {
            AlertaPanico a = alerta.get();
            double lat = seguimiento.map(Seguimiento::getLatitud).orElse(a.getLatitud());
            double lng = seguimiento.map(Seguimiento::getLongitud).orElse(a.getLongitud());
            OffsetDateTime cuando = seguimiento.map(Seguimiento::getActualizadoEn).orElse(a.getCreadoEn());
            return new ProtegidoRespuesta(p.getId(), p.getNombre(), p.getTelefono(), "PANICO", a.getId(),
                    lat, lng, cuando, seguimiento.map(Seguimiento::getExpiraEn).orElse(null));
        }
        if (seguimiento.isPresent()) {
            Seguimiento s = seguimiento.get();
            return new ProtegidoRespuesta(p.getId(), p.getNombre(), p.getTelefono(), "TRAYECTO", null,
                    s.getLatitud(), s.getLongitud(), s.getActualizadoEn(), s.getExpiraEn());
        }
        return new ProtegidoRespuesta(p.getId(), p.getNombre(), p.getTelefono(), "TRANQUILO", null,
                null, null, null, null);
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
    }
}
