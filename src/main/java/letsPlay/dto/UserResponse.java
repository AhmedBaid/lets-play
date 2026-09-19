package letsPlay.dto;

import letsPlay.enums.Role;

public record UserResponse(String id, String name, String email, Role role) {

    public static UserResponse from(letsPlay.models.UserModel user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}