package br.com.notification_service.infra.exception;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
