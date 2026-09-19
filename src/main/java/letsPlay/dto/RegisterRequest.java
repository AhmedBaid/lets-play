package letsPlay.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class RegisterRequest {
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 20, message = "Name must be between 3 and 20 characters")
        private String name;
        @Size(min = 8, max = 30, message = "Email must be between 8 and 30 characters")
        @NotBlank(message = "Email cannot be blank")
        @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Email is not valid")
        private String email;
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 20, message = "Password must be between 3 and 20 characters")
        private String password;
}