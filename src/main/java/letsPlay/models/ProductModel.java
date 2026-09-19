package letsPlay.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "product")
@Getter
@Setter
public class ProductModel {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("price")
    private Double price;

    @Field("description")
    private String description;

    private String userId;
}