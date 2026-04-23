package br.com.api_core.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateDTO(

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes,

        @NotNull(message = "Items are required")
        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemCreateDTO> items
) {
}
