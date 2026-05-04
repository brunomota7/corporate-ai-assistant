package br.com.notification_service.dto;

import java.math.BigDecimal;

public record OrderCancelledEventDTO(
        String orderId,
        String userId,
        String userName,
        String userEmail,
        BigDecimal totalAmount
) {
}
