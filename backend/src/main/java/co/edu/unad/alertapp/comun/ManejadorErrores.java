package co.edu.unad.alertapp.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public record RespuestaError(OffsetDateTime fecha, int estado, String mensaje, Map<String, String> campos) {}

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<RespuestaError> negocio(ErrorNegocio e) {
        return cuerpo(e.getEstado(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return cuerpo(HttpStatus.BAD_REQUEST, "Revisa los campos marcados", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<RespuestaError> formato(Exception e) {
        return cuerpo(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato o un valor no válido", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<RespuestaError> archivoGrande(MaxUploadSizeExceededException e) {
        return cuerpo(HttpStatus.PAYLOAD_TOO_LARGE, "La foto supera el tamaño máximo de 5 MB", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<RespuestaError> acceso(AccessDeniedException e) {
        return cuerpo(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException e) {
        return cuerpo(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<RespuestaError> general(Exception e) {
        log.error("Error no controlado", e);
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.", Map.of());
    }

    private ResponseEntity<RespuestaError> cuerpo(HttpStatus estado, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado).body(new RespuestaError(OffsetDateTime.now(), estado.value(), mensaje, campos));
    }
}
