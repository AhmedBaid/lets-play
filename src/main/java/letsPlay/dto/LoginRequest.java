package letsPlay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
@Getter 
public class LoginRequest {
        @NotBlank(message = "Username or email is required")
        private String name;
        @NotBlank(message = "Password is required")
        private String password;
}