package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class OrderCancellationNotAllowedException extends BusinessException {
    public OrderCancellationNotAllowedException(String message) {
        super("Unauthorized order cancellation", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
