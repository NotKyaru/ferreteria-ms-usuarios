package cl.duoc.ferreteria.usuarios.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Error de negocio con su código HTTP. Los services lanzan subclases de esta; ManejadorErrores las traduce a JSON. */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getDetalles() {
        return null;
    }
}
