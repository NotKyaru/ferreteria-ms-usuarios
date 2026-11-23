package cl.duoc.ferreteria.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 403: hay sesión, pero el rol no permite la acción. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
