package br.com.api_core.modules.chat.dto;

public record ChatResponseDTO(
        String sessionId,
        String answer,
        Integer tokensUsed
) {
}
