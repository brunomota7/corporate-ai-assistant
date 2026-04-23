package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class OrderCancellationNotAllowedException extends BusinessException {
    public OrderCancellationNotAllowedException(String status) {
        super("Unauthorized order cancellation, order status: " + status, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
