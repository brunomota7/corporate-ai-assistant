package br.com.api_core.modules.chat.dto;

import br.com.api_core.domain.enums.MessageRole;

public record MessageDTO(
        MessageRole role,
        String content
) {
}
