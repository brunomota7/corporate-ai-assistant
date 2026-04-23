package br.com.api_core.modules.order.dto;

import br.com.api_core.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponseDTO> items
) {
}
