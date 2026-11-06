package co.edu.unad.alertapp.comunidad;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.config.AlertAppProperties;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Reporte;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReporteRespuesta;
import co.edu.unad.alertapp.reporte.ReporteServicio;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import co.edu.unad.alertapp.usuario.Usuario;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * HU 07 - Validación comunitaria. Cada vecino vota una vez por reporte (puede cambiar su voto).
 * Cuando las confirmaciones superan a los votos falsos por el umbral, el reporte pasa a VALIDADO;
 * en el caso contrario se anula automáticamente (DESCARTADO) como medida anti-spam.
 */
@Service
public class ComunidadServicio {

    private static final Set<Estado> VOTABLES = Set.of(Estado.PENDIENTE, Estado.VALIDADO);

    private final ReporteServicio reportes;
    private final VotoRepositorio votos;
    private final ComentarioRepositorio comentarios;
    private final UsuarioRepositorio usuarios;
    private final int umbral;

    public ComunidadServicio(ReporteServicio reportes, VotoRepositorio votos, ComentarioRepositorio comentarios,
                             UsuarioRepositorio usuarios, AlertAppProperties props) {
        this.reportes = reportes;
        this.votos = votos;
        this.comentarios = comentarios;
        this.usuarios = usuarios;
        this.umbral = props.validacion().umbral();
    }

    @Transactional
    public ReporteRespuesta votar(UsuarioAutenticado actual, Long reporteId, Voto.Tipo tipo) {
        Reporte r = reportes.buscar(reporteId);
        if (r.esDe(actual.id())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "No puedes votar tu propio reporte");
        }
        if (!VOTABLES.contains(r.getEstado())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "Este reporte ya fue cerrado y no recibe votos");
        }
        Usuario votante = usuario(actual);
        votos.findByReporteIdAndUsuarioId(reporteId, actual.id())
                .ifPresentOrElse(v -> v.setTipo(tipo), () -> votos.save(new Voto(r, votante, tipo)));
        votos.flush();
        reportes.recontarVotos(r);
        aplicarRegla(r);
        return ReporteRespuesta.de(r, actual.id(), tipo.name());
    }

    @Transactional
    public ReporteRespuesta quitarVoto(UsuarioAutenticado actual, Long reporteId) {
        Reporte r = reportes.buscar(reporteId);
        votos.findByReporteIdAndUsuarioId(reporteId, actual.id()).ifPresent(votos::delete);
        votos.flush();
        reportes.recontarVotos(r);
        return ReporteRespuesta.de(r, actual.id(), null);
    }

    @Transactional(readOnly = true)
    public List<ComentarioRespuesta> comentarios(UsuarioAutenticado actual, Long reporteId) {
        reportes.buscar(reporteId);
        return comentarios.findByReporteIdOrderByCreadoEnAsc(reporteId).stream()
                .map(c -> ComentarioRespuesta.de(c, actual.id()))
                .toList();
    }

    @Transactional
    public ComentarioRespuesta comentar(UsuarioAutenticado actual, Long reporteId, String texto) {
        Reporte r = reportes.buscar(reporteId);
        Comentario c = comentarios.save(new Comentario(r, usuario(actual), texto.trim(), actual.esAutoridad()));
        return ComentarioRespuesta.de(c, actual.id());
    }

    @Transactional
    public void borrarComentario(UsuarioAutenticado actual, Long comentarioId) {
        Comentario c = comentarios.findById(comentarioId)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "El comentario no existe"));
        if (!c.getUsuario().getId().equals(actual.id())) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "Solo puedes borrar tus comentarios");
        }
        comentarios.delete(c);
    }

    void aplicarRegla(Reporte r) {
        int diferencia = r.getConfirmaciones() - r.getFalsos();
        if (diferencia >= umbral && r.getEstado() == Estado.PENDIENTE) {
            r.setEstado(Estado.VALIDADO);
        } else if (-diferencia >= umbral) {
            r.setEstado(Estado.DESCARTADO);
        }
    }

    private Usuario usuario(UsuarioAutenticado actual) {
        return usuarios.findById(actual.id())
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
    }

    public record ComentarioRespuesta(Long id, String autor, boolean esAutoridad, boolean esMio, String texto,
                                      OffsetDateTime creadoEn) {
        static ComentarioRespuesta de(Comentario c, Long actual) {
            String autor = c.isEsAutoridad() ? "Autoridad: " + c.getUsuario().getNombre() : primerNombre(c.getUsuario().getNombre());
            return new ComentarioRespuesta(c.getId(), autor, c.isEsAutoridad(),
                    c.getUsuario().getId().equals(actual), c.getTexto(), c.getCreadoEn());
        }

        /** En la comunidad solo se muestra el primer nombre del vecino para proteger su identidad. */
        private static String primerNombre(String nombre) {
            String limpio = nombre.trim();
            int espacio = limpio.indexOf(' ');
            return espacio > 0 ? limpio.substring(0, espacio) : limpio;
        }
    }
}
