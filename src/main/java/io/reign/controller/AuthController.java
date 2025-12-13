package io.reign.controller;

import io.reign.dto.AuthResponse;
import io.reign.dto.LoginRequest;
import io.reign.dto.RegisterRequest;
import io.reign.model.User;
import io.reign.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(request);
        setAuthCookie(response, authResponse.getToken());

        // Remove token from response body for security
        authResponse.setToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        setAuthCookie(response, authResponse.getToken());

        // Remove token from response body for security
        authResponse.setToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> createGuest(HttpServletResponse response) {
        AuthResponse authResponse = authService.createGuestUser();
        setAuthCookie(response, authResponse.getToken());

        // Remove token from response body for security
        authResponse.setToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(AuthResponse.builder()
                .token(null) // Never send token in response
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole().name())
                .userType(user.getUserType().name())
                .build());
    }

    @GetMapping("/ws-token")
    public ResponseEntity<String> getWebSocketToken(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // Generate a token specifically for WebSocket connection
        // This is needed because SockJS can't send httpOnly cookies in STOMP headers
        String token = authService.generateTokenForUser(user);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Clear the auth cookie
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete immediately
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logout/{userId}")
    public ResponseEntity<Void> deleteGuestUser(
            @PathVariable String userId,
            @AuthenticationPrincipal User authenticatedUser,
            HttpServletResponse response
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

        // Clear the auth cookie
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete immediately
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
