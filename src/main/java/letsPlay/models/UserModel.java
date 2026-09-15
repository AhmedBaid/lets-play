package letsPlay.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import letsPlay.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "user")
@Getter
@Setter
public class UserModel {
    @Id
    private String id;

    @Field("name")
    private String name;
    
    @Field("email")
    private String email;
    
    @Field("password")
    private String password;

    @Field("role")
    private Role role;
}