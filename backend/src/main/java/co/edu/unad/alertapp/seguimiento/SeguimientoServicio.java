package co.edu.unad.alertapp.seguimiento;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.guardian.GuardianServicio;
import co.edu.unad.alertapp.notificacion.Notificacion;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * HU 06 - Rutas frecuentes y modo seguimiento. El usuario comparte su ubicación con sus guardianes hasta por
 * una hora; la app envía la posición cada pocos segundos y el usuario puede detenerlo en cualquier momento.
 */
@Service
public class SeguimientoServicio {

    public static final int MAXIMO_RUTAS = 10;

    private final SeguimientoRepositorio seguimientos;
    private final RutaFrecuenteRepositorio rutas;
    private final UsuarioRepositorio usuarios;
    private final GuardianServicio guardianes;

    public SeguimientoServicio(SeguimientoRepositorio seguimientos, RutaFrecuenteRepositorio rutas,
                               UsuarioRepositorio usuarios, GuardianServicio guardianes) {
        this.seguimientos = seguimientos;
        this.rutas = rutas;
        this.usuarios = usuarios;
        this.guardianes = guardianes;
    }

    public record RutaPeticion(String nombre, double origenLatitud, double origenLongitud,
                               double destinoLatitud, double destinoLongitud, ModoTransporte modo) {}

    public record RutaRespuesta(Long id, String nombre, double origenLatitud, double origenLongitud,
                                double destinoLatitud, double destinoLongitud, ModoTransporte modo) {
        static RutaRespuesta de(RutaFrecuente r) {
            return new RutaRespuesta(r.getId(), r.getNombre(), r.getOrigenLatitud(), r.getOrigenLongitud(),
                    r.getDestinoLatitud(), r.getDestinoLongitud(), r.getModo());
        }
    }

    public record SeguimientoRespuesta(Long id, Seguimiento.Motivo motivo, Long rutaId, String rutaNombre,
                                       double latitud, double longitud, OffsetDateTime iniciadoEn,
                                       OffsetDateTime actualizadoEn, OffsetDateTime expiraEn, boolean activo,
                                       int guardianesAvisados) {
        static SeguimientoRespuesta de(Seguimiento s, int avisados) {
            return new SeguimientoRespuesta(s.getId(), s.getMotivo(),
                    s.getRuta() == null ? null : s.getRuta().getId(),
                    s.getRuta() == null ? null : s.getRuta().getNombre(),
                    s.getLatitud(), s.getLongitud(), s.getIniciadoEn(), s.getActualizadoEn(), s.getExpiraEn(),
                    s.activo(), avisados);
        }
    }

    // ---------- Rutas frecuentes ----------

    @Transactional(readOnly = true)
    public List<RutaRespuesta> rutas(Long usuarioId) {
        return rutas.findByUsuarioIdOrderByNombre(usuarioId).stream().map(RutaRespuesta::de).toList();
    }

    @Transactional
    public RutaRespuesta guardarRuta(Long usuarioId, RutaPeticion p) {
        if (rutas.countByUsuarioId(usuarioId) >= MAXIMO_RUTAS) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Puedes guardar hasta " + MAXIMO_RUTAS + " rutas");
        }
        RutaFrecuente r = new RutaFrecuente(usuario(usuarioId));
        aplicar(r, p);
        return RutaRespuesta.de(rutas.save(r));
    }

    @Transactional
    public RutaRespuesta editarRuta(Long usuarioId, Long id, RutaPeticion p) {
        RutaFrecuente r = ruta(usuarioId, id);
        aplicar(r, p);
        return RutaRespuesta.de(r);
    }

    @Transactional
    public void borrarRuta(Long usuarioId, Long id) {
        rutas.delete(ruta(usuarioId, id));
    }

    // ---------- Seguimiento ----------

    @Transactional
    public SeguimientoRespuesta iniciar(Long usuarioId, int minutos, Long rutaId, double lat, double lng) {
        if (minutos < 5 || minutos > Seguimiento.MAXIMO.toMinutes()) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "El seguimiento dura entre 5 y 60 minutos");
        }
        Usuario u = usuario(usuarioId);
        RutaFrecuente ruta = rutaId == null ? null : ruta(usuarioId, rutaId);
        seguimientos.activo(usuarioId).ifPresent(Seguimiento::detener);
        Seguimiento s = seguimientos.save(new Seguimiento(u, Seguimiento.Motivo.TRAYECTO, ruta, lat, lng,
                Duration.ofMinutes(minutos)));
        String destino = ruta == null ? "" : " (" + ruta.getNombre() + ")";
        int avisados = guardianes.notificar(usuarioId, Notificacion.Tipo.SEGUIMIENTO_PROTEGIDO,
                u.getNombre() + " compartió su trayecto" + destino,
                "Puedes ver dónde va durante los próximos " + minutos + " minutos.", lat, lng);
        return SeguimientoRespuesta.de(s, avisados);
    }

    /** Lo usa el botón de pánico: abre un seguimiento de 1 hora para que los guardianes vean a la persona moverse. */
    @Transactional
    public void iniciarPorPanico(Usuario u, double lat, double lng) {
        seguimientos.activo(u.getId()).ifPresent(Seguimiento::detener);
        seguimientos.save(new Seguimiento(u, Seguimiento.Motivo.PANICO, null, lat, lng, Seguimiento.MAXIMO));
    }

    @Transactional
    public SeguimientoRespuesta mover(Long usuarioId, double lat, double lng) {
        Seguimiento s = seguimientos.activo(usuarioId)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.CONFLICT, "No estás compartiendo tu ubicación"));
        s.mover(lat, lng);
        guardianes.difundirPosicion(usuarioId, lat, lng);
        return SeguimientoRespuesta.de(s, 0);
    }

    @Transactional
    public Optional<SeguimientoRespuesta> detener(Long usuarioId) {
        return seguimientos.activo(usuarioId).map(s -> {
            s.detener();
            guardianes.difundirEstado(usuarioId);
            return SeguimientoRespuesta.de(s, 0);
        });
    }

    @Transactional
    public void detenerPorPanico(Long usuarioId) {
        seguimientos.activo(usuarioId).filter(s -> s.getMotivo() == Seguimiento.Motivo.PANICO).ifPresent(Seguimiento::detener);
        // Aunque no hubiera seguimiento, la alerta sí cambió: el guardián debe dejar de verla en pánico.
        guardianes.difundirEstado(usuarioId);
    }

    @Transactional(readOnly = true)
    public Optional<SeguimientoRespuesta> actual(Long usuarioId) {
        return seguimientos.activo(usuarioId).map(s -> SeguimientoRespuesta.de(s, 0));
    }

    private void aplicar(RutaFrecuente r, RutaPeticion p) {
        if (p.nombre() == null || p.nombre().isBlank() || p.nombre().length() > 60) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "Ponle un nombre corto a la ruta (máximo 60 letras)");
        }
        if (p.modo() == null) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, "Elige cómo te desplazas");
        }
        r.actualizar(p.nombre().trim(), p.origenLatitud(), p.origenLongitud(), p.destinoLatitud(),
                p.destinoLongitud(), p.modo());
    }

    private RutaFrecuente ruta(Long usuarioId, Long id) {
        return rutas.findById(id)
                .filter(r -> r.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "La ruta no existe"));
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
    }
}
