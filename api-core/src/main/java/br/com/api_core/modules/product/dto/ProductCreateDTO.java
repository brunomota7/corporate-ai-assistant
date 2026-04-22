package br.com.api_core.modules.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Invalid price format")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity must be zero or greater")
        Integer stockQuantity,

        @Size(max = 80, message = "Category must not exceed 80 characters")
        String category
) {}
