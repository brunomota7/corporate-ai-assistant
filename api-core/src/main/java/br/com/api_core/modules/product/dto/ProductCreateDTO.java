package br.com.api_core.modules.product.dto;

import java.math.BigDecimal;

public record ProductCreateDTO(
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String category
) {
}
