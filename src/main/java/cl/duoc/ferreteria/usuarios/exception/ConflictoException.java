package cl.duoc.ferreteria.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 409: la operación choca con el estado actual (correo o RUT duplicado, último administrador...). */
public class ConflictoException extends NegocioException {

    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
