package letsPlay.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import letsPlay.models.UserModel;

public interface UserRepository extends MongoRepository<UserModel, String> {
    Optional<UserModel> findByName(String name);

    Optional<UserModel> findByEmail(String email);

    boolean existsByName(String name);

    boolean existsByEmail(String email);
}