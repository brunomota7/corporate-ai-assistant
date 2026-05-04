package br.com.notification_service.dto;

public record UserRegisteredEventDTO(
        String userId,
        String name,
        String email
) {
}
