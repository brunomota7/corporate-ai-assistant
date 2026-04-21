package br.com.api_core.infra.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BusinessException {
    public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email, HttpStatus.CONFLICT);
    }
}
