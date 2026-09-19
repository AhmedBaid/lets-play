package letsPlay.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import letsPlay.config.JwtUtil;
import letsPlay.dto.LoginRequest;
import letsPlay.dto.LoginResponseDTO;
import letsPlay.dto.RegisterRequest;
import letsPlay.dto.ResponseDTO;
import letsPlay.enums.Role;
import letsPlay.exception.GlobalException;
import letsPlay.models.UserModel;
import letsPlay.repository.UserRepository;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public ResponseDTO register(RegisterRequest request) {
        String name = request.getName().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByName(name)) {
            throw new GlobalException("Username '" + name + "' is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(email)) {
            throw new GlobalException("Email '" + email + "' is already registered", HttpStatus.CONFLICT);
        }

        UserModel user = new UserModel();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
        return new ResponseDTO("user registred successfully");
    }

    public LoginResponseDTO login(LoginRequest request) {
        UserModel user = findByIdentifier(request.getName())
                .orElseThrow(() -> new GlobalException("Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new GlobalException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        String token = jwtUtil.generateToken(user.getName(), user.getRole());
        return new LoginResponseDTO(token);
    }

    private java.util.Optional<UserModel> findByIdentifier(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return userRepository.findByEmail(value.toLowerCase());
        }
        return userRepository.findByName(value);
    }
}