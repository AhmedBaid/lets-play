package letsPlay.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import letsPlay.models.UserModel;

public interface UserRepository extends MongoRepository<UserModel, String> {
    Optional<UserModel> findByName(String name);
}