package letsPlay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Product name is required") @Size(max = 100, message = "Product name cannot exceed 100 characters") String name,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.0", message = "Price cannot be negative") Double price,
        @Size(max = 1000, message = "Description cannot exceed 1000 characters") String description) {
}