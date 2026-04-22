package br.com.api_core.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateDTO(

        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Invalid price format")
        BigDecimal price,

        @Min(value = 0, message = "Stock quantity must be zero or greater")
        Integer stockQuantity,

        @Size(max = 80, message = "Category must not exceed 80 characters")
        String category
) {}
