package edu.casas.budgetbuddy.features.auth;

import edu.casas.budgetbuddy.features.auth.AuthDtos.ChangePasswordRequest;
import edu.casas.budgetbuddy.features.auth.AuthDtos.LoginRequest;
import edu.casas.budgetbuddy.features.auth.AuthDtos.RefreshTokenRequest;
import edu.casas.budgetbuddy.features.auth.AuthDtos.RegisterRequest;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.AuthData>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                authService.register(request.email(), request.password(), request.firstname(), request.lastname()),
                "User registered successfully"));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.email(), request.password()), "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.AuthData> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()), "Session refreshed");
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.AuthUser> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(authService.toAuthUser(authService.requireUser(authorization)), "Profile loaded");
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authorization, request.currentPassword(), request.newPassword());
        return ApiResponse.success(null, "Password updated");
    }

    @GetMapping("/google")
    public ResponseEntity<Void> google() {
        return ResponseEntity.status(302).location(URI.create("/api/v1/auth/google/callback")).build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<ApiResponse<AuthDtos.AuthData>> googleCallback(
            @RequestParam(defaultValue = "google.user@example.com") String email,
            @RequestParam(defaultValue = "Google") String firstname,
            @RequestParam(defaultValue = "User") String lastname,
            @RequestParam(defaultValue = "mock-google-id") String googleId) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.loginWithGoogle(email, firstname, lastname, googleId),
                "Google login successful"));
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.status(302).location(URI.create("/")).build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logoutPost(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.success(null, "Logged out");
    }
}
