package letsPlay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequest {
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 20, message = "Name must be between 3 and 20 characters")
        private String name;
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 20, message = "Password must be between 3 and 20 characters")
        private String password;
}