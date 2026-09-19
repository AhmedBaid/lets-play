package letsPlay.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import letsPlay.dto.UpdateUserRequest;
import letsPlay.enums.Role;
import letsPlay.exception.GlobalException;
import letsPlay.models.UserModel;
import letsPlay.repository.ProductRepository;
import letsPlay.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ProductRepository productRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    public UserModel getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new GlobalException("User not found", HttpStatus.NOT_FOUND));
    }

    public UserModel updateUser(String id, UpdateUserRequest request) {
        UserModel user = getUser(id);

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equals(user.getName()) && userRepository.existsByName(name)) {
                throw new GlobalException("Username '" + name + "' is already taken", HttpStatus.CONFLICT);
            }
            user.setName(name);
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new GlobalException("Email '" + email + "' is already registered", HttpStatus.CONFLICT);
            }
            user.setEmail(email);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.role().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new GlobalException("Role must be one of: ADMIN, USER", HttpStatus.BAD_REQUEST);
            }
        }

        return userRepository.save(user);
    }

    /**
     * Deletes a user along with every product they own. An admin cannot delete
     * their own account through this endpoint.
     */
    public void deleteUser(String id, String callerName) {
        UserModel user = getUser(id);

        if (user.getName().equals(callerName)) {
            throw new GlobalException("You cannot delete your own account", HttpStatus.BAD_REQUEST);
        }

        productRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }
}