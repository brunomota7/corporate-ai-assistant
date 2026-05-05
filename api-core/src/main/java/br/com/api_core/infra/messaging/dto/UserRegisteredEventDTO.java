package br.com.api_core.infra.messaging.dto;

public record UserRegisteredEventDTO(
        String userId,
        String name,
        String email
) {
}
