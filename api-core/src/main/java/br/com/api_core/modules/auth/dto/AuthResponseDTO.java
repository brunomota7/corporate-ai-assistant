package br.com.api_core.modules.auth.dto;

public record AuthResponseDTO(
        String token,
        String name,
        String email,
        String role
) {
}
