package br.com.api_core.modules.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String category,
        Boolean active
) {
}
