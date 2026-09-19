package letsPlay.dto;

import java.time.LocalDateTime;

public record AuthResponse(String token, String type, LocalDateTime expiresAt, UserResponse user) {

    public AuthResponse(String token, LocalDateTime expiresAt, UserResponse user) {
        this(token, "Bearer", expiresAt, user);
    }
}