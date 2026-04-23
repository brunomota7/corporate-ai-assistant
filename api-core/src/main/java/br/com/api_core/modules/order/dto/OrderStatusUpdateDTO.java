package br.com.api_core.modules.order.dto;

import br.com.api_core.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateDTO(

        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
