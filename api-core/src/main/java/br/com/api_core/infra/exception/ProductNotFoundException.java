package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String productId) {
        super("Product not found with id: " + productId, HttpStatus.NOT_FOUND);
    }
}
