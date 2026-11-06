package co.edu.unad.alertapp.comun;

import org.springframework.http.HttpStatus;

/** Error esperado de una regla de negocio; se traduce a una respuesta HTTP con mensaje legible. */
public class ErrorNegocio extends RuntimeException {

    private final HttpStatus estado;

    public ErrorNegocio(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
