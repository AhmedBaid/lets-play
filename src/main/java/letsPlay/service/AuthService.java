package letsPlay.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import letsPlay.config.JwtUtil;
import letsPlay.dto.AuthResponse;
import letsPlay.dto.LoginRequest;
import letsPlay.dto.RegisterRequest;
import letsPlay.dto.UserResponse;
import letsPlay.enums.Role;
import letsPlay.exception.GlobalException;
import letsPlay.models.UserModel;
import letsPlay.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long jwtExpiration;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Value("${application.security.jwt.expiration}") long jwtExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtExpiration = jwtExpiration;
    }

    public AuthResponse register(RegisterRequest request) {
        String name = request.name().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByName(name)) {
            throw new GlobalException("Username '" + name + "' is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(email)) {
            throw new GlobalException("Email '" + email + "' is already registered", HttpStatus.CONFLICT);
        }

        UserModel user = new UserModel();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserModel user = findByIdentifier(request.name())
                .orElseThrow(() -> new GlobalException("Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new GlobalException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserModel user) {
        String token = jwtUtil.generateToken(user.getName(), user.getRole());
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(jwtExpiration));
        return new AuthResponse(token, expiresAt, UserResponse.from(user));
    }

    private java.util.Optional<UserModel> findByIdentifier(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return userRepository.findByEmail(value.toLowerCase());
        }
        return userRepository.findByName(value);
    }
}