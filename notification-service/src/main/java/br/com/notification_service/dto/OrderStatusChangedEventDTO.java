package br.com.notification_service.dto;

public record OrderStatusChangedEventDTO(
        String orderId,
        String userId,
        String userName,
        String userEmail,
        String oldStatus,
        String newStatus
) {
}
