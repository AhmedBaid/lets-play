package letsPlay.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or email is required") String name,
        @NotBlank(message = "Password is required") String password) {
}