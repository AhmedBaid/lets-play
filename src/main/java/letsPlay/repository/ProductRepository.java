package letsPlay.repository;


import org.springframework.data.mongodb.repository.MongoRepository;

import letsPlay.models.ProductModel;

public interface ProductRepository extends MongoRepository<ProductModel, String> {
    void deleteByUserId(String userId);
}