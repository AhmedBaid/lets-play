package letsPlay.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import letsPlay.dto.UpdateUserRequest;
import letsPlay.enums.Role;
import letsPlay.exception.GlobalException;
import letsPlay.models.UserModel;
import letsPlay.repository.ProductRepository;
import letsPlay.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    public UserModel getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new GlobalException("User not found", HttpStatus.NOT_FOUND));
    }

    public UserModel updateUser(String id, UpdateUserRequest request) {
        UserModel user = getUser(id);

        String name = request.getName().trim();
        if (!name.equals(user.getName()) && userRepository.existsByName(name)) {
            throw new GlobalException("Username '" + name + "' is already taken", HttpStatus.CONFLICT);
        }
        user.setName(name);

        String email = request.getEmail().trim().toLowerCase();
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new GlobalException("Email '" + email + "' is already registered", HttpStatus.CONFLICT);
        }
        user.setEmail(email);

        user.setRole(Role.valueOf(request.getRole().trim().toUpperCase()));

        return userRepository.save(user);
    }

    public void deleteUser(String id, String callerName) {
        UserModel user = getUser(id);

        if (user.getName().equals(callerName)) {
            throw new GlobalException("You cannot delete your own account", HttpStatus.BAD_REQUEST);
        }

        productRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }
}