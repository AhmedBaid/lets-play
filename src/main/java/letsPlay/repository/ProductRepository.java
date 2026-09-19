package letsPlay.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import letsPlay.models.ProductModel;

public interface ProductRepository extends MongoRepository<ProductModel, String> {
    List<ProductModel> findByUserId(String userId);

    void deleteByUserId(String userId);
}