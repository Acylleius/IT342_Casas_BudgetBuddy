package edu.casas.budgetbuddy.controller;

import edu.casas.budgetbuddy.dto.AuthResponse;
import edu.casas.budgetbuddy.dto.LoginRequest;
import edu.casas.budgetbuddy.dto.RegisterRequest;
import edu.casas.budgetbuddy.entity.User;
import edu.casas.budgetbuddy.factory.ResponseFactory;
import edu.casas.budgetbuddy.service.UserService;
import edu.casas.budgetbuddy.strategy.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthContext authContext;

    public AuthController(UserService userService, AuthContext authContext) {
        this.userService = userService;
        this.authContext = authContext;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ResponseFactory.error("EMAIL_EXISTS", "Email already exists", "Use a different email address."));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userService.register(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseFactory.success("User registered successfully", savedUser));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authContext.authenticate(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }
}
