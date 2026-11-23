package cl.duoc.ferreteria.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 401: credenciales incorrectas o sesión que ya no sirve. */
public class NoAutorizadoException extends NegocioException {

    public NoAutorizadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
