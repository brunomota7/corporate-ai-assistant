package br.com.api_core.infra.messaging.dto;

import java.math.BigDecimal;

public record OrderCancelledEventDTO(
        String orderId,
        String userId,
        String userName,
        String userEmail,
        BigDecimal totalAmount
) {
}
