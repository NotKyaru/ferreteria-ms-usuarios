package cl.duoc.ferreteria.usuarios.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso pedido no existe. */
public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
