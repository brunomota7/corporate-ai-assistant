package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class ProductAlreadyExistsException extends BusinessException {
    public ProductAlreadyExistsException(String sku) {
        super("Product already exists with sku: " + sku, HttpStatus.CONFLICT);
    }
}
