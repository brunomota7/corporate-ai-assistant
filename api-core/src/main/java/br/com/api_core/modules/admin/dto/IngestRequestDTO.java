package br.com.api_core.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record IngestRequestDTO(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        @NotBlank(message = "Source type is required")
        String sourceType,

        UUID sourceId,

        Map<String, Object> metadata
) {
}
