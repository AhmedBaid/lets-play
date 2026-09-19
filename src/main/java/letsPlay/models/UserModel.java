package letsPlay.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @Indexed(unique = true)
    private String name;

    @Field("email")
    @Indexed(unique = true)
    private String email;

    @Field("password")
    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Field("role")
    private Role role;
}