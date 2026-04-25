package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends BusinessException {
    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
