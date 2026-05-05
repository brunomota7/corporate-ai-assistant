package br.com.api_core.infra.messaging.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmedEventDTO(
        String orderId,
        String userId,
        String userName,
        String userEmail,
        BigDecimal totalAmount,
        String notes,
        List<OrderItemDTO> items
) {

    public record OrderItemDTO(
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
