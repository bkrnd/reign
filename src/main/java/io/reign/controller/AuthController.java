package io.reign.controller;

import io.reign.dto.AuthResponse;
import io.reign.dto.LoginRequest;
import io.reign.dto.RegisterRequest;
import io.reign.model.User;
import io.reign.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> createGuest() {
        return ResponseEntity.ok(authService.createGuestUser());
    }

    @DeleteMapping("/logout/{userId}")
    public ResponseEntity<Void> deleteGuestUser(
            @PathVariable String userId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Only allow users to delete their own account
        if (!authenticatedUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // Only allow guest users to be deleted
        if (authenticatedUser.getUserType() != io.reign.enums.UserType.GUEST) {
            return ResponseEntity.status(403).build();
        }

        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
