package letsPlay.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;

@Getter
public class UpdateUserRequest {
        @Size(min = 3, max = 20, message = "Name must be between 3 and 20 characters")
        private String name;
        @Size(min = 8, max = 30, message = "Email must be between 8 and 30 characters")
        @NotBlank(message = "Email cannot be blank")
        @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Email is not valid")
        private String email;
        @Pattern(regexp = "USER|ADMIN", message = "Role must be either USER or ADMIN")
        @NotBlank(message = "Role must not be blank")
        private String role;
}