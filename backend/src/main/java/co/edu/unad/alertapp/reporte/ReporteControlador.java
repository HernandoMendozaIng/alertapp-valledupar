package co.edu.unad.alertapp.reporte;

import co.edu.unad.alertapp.comun.ErrorNegocio;
import co.edu.unad.alertapp.reporte.Catalogos.Estado;
import co.edu.unad.alertapp.reporte.Catalogos.Severidad;
import co.edu.unad.alertapp.reporte.Catalogos.TipoDelito;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReportePeticion;
import co.edu.unad.alertapp.reporte.ReporteDtos.ReporteRespuesta;
import co.edu.unad.alertapp.seguridad.UsuarioAutenticado;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** HU 05 - Reporte guiado con evidencia, y HU 03 / HU 08 - consulta con filtros para el mapa. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteControlador {

    private final ReporteServicio servicio;
    private final AlmacenEvidencias almacen;
    private final Validator validador;

    public ReporteControlador(ReporteServicio servicio, AlmacenEvidencias almacen, Validator validador) {
        this.servicio = servicio;
        this.almacen = almacen;
        this.validador = validador;
    }

    @GetMapping
    public List<ReporteRespuesta> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) List<Estado> estado,
            @RequestParam(required = false) Short comuna,
            @RequestParam(required = false) TipoDelito tipo,
            @RequestParam(required = false) Severidad severidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radio) {
        return servicio.listar(usuario, new FiltroReportes(estado, comuna, tipo, severidad, desde, hasta, lat, lng, radio));
    }

    @GetMapping("/mios")
    public List<ReporteRespuesta> mios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.mios(usuario);
    }

    @GetMapping("/{id}")
    public ReporteRespuesta detalle(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return servicio.detalle(usuario, id);
    }

    /** Crea un reporte. Acepta JSON simple o multipart con la parte "datos" y una "foto" opcional. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteRespuesta crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                  @Valid @RequestBody ReportePeticion peticion) {
        return servicio.crear(usuario, peticion, null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteRespuesta crearConFoto(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @RequestPart("datos") ReportePeticion peticion,
                                         @RequestPart(value = "foto", required = false) MultipartFile foto) {
        Set<ConstraintViolation<ReportePeticion>> errores = validador.validate(peticion);
        if (!errores.isEmpty()) {
            throw new ErrorNegocio(HttpStatus.BAD_REQUEST, errores.iterator().next().getMessage());
        }
        return servicio.crear(usuario, peticion, foto);
    }

    @PutMapping("/{id}")
    public ReporteRespuesta actualizar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                       @Valid @RequestBody ReportePeticion peticion) {
        return servicio.actualizar(usuario, id, peticion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        servicio.eliminar(usuario, id);
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> evidencia(@PathVariable Long id) {
        Path ruta = servicio.evidencia(id);
        return ResponseEntity.ok()
                .contentType(almacen.tipo(ruta.getFileName().toString()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(new FileSystemResource(ruta));
    }
}
