package io.reign.service;

import io.reign.dto.AuthResponse;
import io.reign.dto.LoginRequest;
import io.reign.dto.RegisterRequest;
import io.reign.enums.Role;
import io.reign.enums.UserType;
import io.reign.model.User;
import io.reign.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TeamService teamService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(UserType.REGISTERED);
        user.setRole(Role.USER);

        user = userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole().name())
                .userType(user.getUserType().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole().name())
                .userType(user.getUserType().name())
                .build();
    }

    public AuthResponse createGuestUser() {
        String guestUsername = "guest_" + UUID.randomUUID().toString().substring(0, 8);

        User guestUser = new User();
        guestUser.setUsername(guestUsername);
        guestUser.setPassword(null);
        guestUser.setUserType(UserType.GUEST);
        guestUser.setRole(Role.USER);

        guestUser = userRepository.save(guestUser);

        String jwtToken = jwtService.generateToken(guestUser);

        return AuthResponse.builder()
                .token(jwtToken)
                .username(guestUser.getUsername())
                .userId(guestUser.getId())
                .role(guestUser.getRole().name())
                .userType(guestUser.getUserType().name())
                .build();
    }

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Cleanup user data (squares, team memberships, empty teams)
        teamService.cleanupUserData(userId);

        // Delete the user
        userRepository.delete(user);
    }

    public String generateTokenForUser(User user) {
        return jwtService.generateToken(user);
    }
}
