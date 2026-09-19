package letsPlay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProductRequest {
        @NotBlank(message = "Product name is required")
        @Size(max = 50, message = "Product name cannot exceed 50 characters")
        private String name;
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        private Double price;
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
}