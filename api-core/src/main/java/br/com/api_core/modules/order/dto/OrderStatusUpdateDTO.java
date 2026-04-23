package br.com.api_core.modules.order.dto;

import br.com.api_core.domain.enums.OrderStatus;

public record OrderStatusUpdateDTO(
        OrderStatus status
) {
}
