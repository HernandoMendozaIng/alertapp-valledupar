package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.comunidad.Voto;
import co.edu.unad.alertapp.comunidad.VotoRepositorio;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReportePeticion;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReporteRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import co.edu.unad.alertapp.usuario.UsuarioRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReporteServicio {

    /** Estados que un ciudadano todavía puede editar o borrar. */
    private static final Set<Estado> EDITABLES = Set.of(Estado.PENDIENTE);
    private static final int MAXIMO_LISTADO = 500;

    private final ReporteRepositorio reportes;
    private final UsuarioRepositorio usuarios;
    private final VotoRepositorio votos;
    private final ClasificadorSeveridad clasificador;
    private final AlmacenEvidencias almacen;

    public ReporteServicio(ReporteRepositorio reportes, UsuarioRepositorio usuarios, VotoRepositorio votos,
                           ClasificadorSeveridad clasificador, AlmacenEvidencias almacen) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.votos = votos;
        this.clasificador = clasificador;
        this.almacen = almacen;
    }

    @Transactional
    public ReporteRespuesta crear(UsuarioAutenticado actual, ReportePeticion p, MultipartFile foto) {
        var autor = usuarios.findById(actual.id())
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"));
        Reporte r = new Reporte(autor);
        aplicar(r, p);
        r.setEvidenciaArchivo(almacen.guardar(foto));
        return ReporteRespuesta.de(reportes.save(r), actual.id(), null);
    }

    @Transactional
    public ReporteRespuesta actualizar(UsuarioAutenticado actual, Long id, ReportePeticion p) {
        Reporte r = buscar(id);
        exigirAutorEditable(actual, r);
        aplicar(r, p);
        return ReporteRespuesta.de(r, actual.id(), null);
    }

    @Transactional
    public void eliminar(UsuarioAutenticado actual, Long id) {
        Reporte r = buscar(id);
        exigirAutorEditable(actual, r);
        almacen.borrar(r.getEvidenciaArchivo());
        reportes.delete(r);
    }

    @Transactional(readOnly = true)
    public ReporteRespuesta detalle(UsuarioAutenticado actual, Long id) {
        Reporte r = buscar(id);
        String miVoto = votos.findByReporteIdAndUsuarioId(id, actual.id()).map(v -> v.getTipo().name()).orElse(null);
        return ReporteRespuesta.de(r, actual.id(), miVoto);
    }

    @Transactional(readOnly = true)
    public List<ReporteRespuesta> listar(UsuarioAutenticado actual, FiltroReportes filtro) {
        var pagina = PageRequest.of(0, MAXIMO_LISTADO, Sort.by(Sort.Direction.DESC, "fechaHecho"));
        List<Reporte> encontrados = reportes.findAll(filtro.especificacion(), pagina).getContent().stream()
                .filter(filtro::dentroDelRadio)
                .toList();
        return conVotos(actual, encontrados);
    }

    @Transactional(readOnly = true)
    public List<ReporteRespuesta> mios(UsuarioAutenticado actual) {
        return conVotos(actual, reportes.findByAutorIdOrderByCreadoEnDesc(actual.id()));
    }

    @Transactional(readOnly = true)
    public Path evidencia(Long id) {
        Reporte r = buscar(id);
        if (r.getEvidenciaArchivo() == null) {
            throw new ErrorNegocio(HttpStatus.NOT_FOUND, "Este reporte no tiene foto");
        }
        return almacen.ruta(r.getEvidenciaArchivo());
    }

    public Reporte buscar(Long id) {
        return reportes.findById(id)
                .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "El reporte no existe o fue eliminado"));
    }

    private List<ReporteRespuesta> conVotos(UsuarioAutenticado actual, List<Reporte> lista) {
        if (lista.isEmpty()) return List.of();
        Map<Long, String> mis = votos.deUsuarioEnReportes(actual.id(), lista.stream().map(Reporte::getId).toList())
                .stream().collect(Collectors.toMap(v -> v.getReporte().getId(), v -> v.getTipo().name()));
        return lista.stream().map(r -> ReporteRespuesta.de(r, actual.id(), mis.get(r.getId()))).toList();
    }

    private void exigirAutorEditable(UsuarioAutenticado actual, Reporte r) {
        if (!r.esDe(actual.id())) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "Solo quien creó el reporte puede modificarlo");
        }
        if (!EDITABLES.contains(r.getEstado())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "El reporte ya fue validado o revisado y no se puede modificar");
        }
    }

    private void aplicar(Reporte r, ReportePeticion p) {
        r.setTipoDelito(p.tipoDelito());
        r.setArma(p.arma());
        r.setMedio(p.medio());
        r.setElementoHurtado(p.elementoHurtado());
        r.setDescripcion(p.descripcion() == null || p.descripcion().isBlank() ? null : p.descripcion().trim());
        r.setFechaHecho(p.fechaHecho());
        r.setLatitud(p.latitud());
        r.setLongitud(p.longitud());
        r.setComuna(p.comuna());
        r.setBarrio(p.barrio() == null || p.barrio().isBlank() ? null : p.barrio().trim());
        r.setAnonimo(p.anonimo());
        var resultado = clasificador.clasificar(p.tipoDelito(), p.arma(), p.medio(), p.elementoHurtado());
        r.setSeveridad(resultado.severidad(), resultado.puntaje());
    }

    /** Usado por la validación comunitaria para recalcular el conteo de votos. */
    @Transactional
    public void recontarVotos(Reporte r) {
        int confirma = (int) votos.countByReporteIdAndTipo(r.getId(), Voto.Tipo.CONFIRMA);
        int falso = (int) votos.countByReporteIdAndTipo(r.getId(), Voto.Tipo.FALSO);
        r.setVotos(confirma, falso);
    }
}
