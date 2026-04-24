package br.com.api_core.modules.audit.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponseDTO(
        UUID id,
        String sessionId,
        String question,
        String answer,
        Integer tokenUsed,
        Integer latencyMs,
        String ipAddress,
        LocalDateTime createdAt
) {
}
