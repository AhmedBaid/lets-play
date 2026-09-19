package letsPlay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters") String name,
        @Email(message = "Email must be valid") String email,
        @Size(min = 6, message = "Password must be at least 6 characters") String password,
        String role) {
}