package br.com.api_core.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100,message = "Name must not exceed 100 characters")
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ0-9\\s.,-]+$", message = "Invalid characters")
        String name,

        @Email(message = "Invalid email format")
        @Size(min = 6, max = 180, message = "Email must be between 6 and 180 characters")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password
) {
}
