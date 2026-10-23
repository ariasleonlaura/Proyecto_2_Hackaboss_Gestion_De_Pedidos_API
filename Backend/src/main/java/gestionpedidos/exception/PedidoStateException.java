package gestionpedidos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PedidoStateException extends RuntimeException {
    public PedidoStateException(String message) {
        super(message);
    }
}
