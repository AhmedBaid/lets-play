package letsPlay.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import letsPlay.dto.UpdateUserRequest;
import letsPlay.models.UserModel;
import letsPlay.service.UserService;

/**
 * Admin-only user management. The whole /api/admin/** tree is restricted to
 * users with the ADMIN role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/admin/users
     * Admin only. Lists all users (passwords are never serialized).
     */
    @GetMapping
    public ResponseEntity<List<UserModel>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/admin/users/{id}
     * Admin only. Returns a single user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserModel> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    /**
     * PUT /api/admin/users/{id}
     * Admin only. Updates name/email/password/role of any user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserModel> updateUser(@PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * DELETE /api/admin/users/{id}
     * Admin only. Deletes the user and all their products.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id, currentUsername());
        return ResponseEntity.noContent().build();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}