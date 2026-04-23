package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String message) {
        super("Order not found", HttpStatus.NOT_FOUND);
    }
}
