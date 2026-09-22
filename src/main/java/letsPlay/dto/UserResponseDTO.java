package letsPlay.dto;

import letsPlay.enums.Role;
import letsPlay.models.UserModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String name;
    private String email;
    private Role role;

    public static UserResponseDTO from(UserModel user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}