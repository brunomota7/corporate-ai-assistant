package br.com.api_core.infra.messaging.dto;

public record OrderStatusChangedEventDTO(
        String orderId,
        String userId,
        String userName,
        String userEmail,
        String oldStatus,
        String newStatus
) {
}
