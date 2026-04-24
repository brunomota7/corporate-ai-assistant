package br.com.api_core.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(

        @NotBlank(message = "Message is required")
        String message,
        String sessionId
) {
}
