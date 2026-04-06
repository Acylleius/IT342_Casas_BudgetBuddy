package edu.casas.budgetbuddy.strategy;

import edu.casas.budgetbuddy.dto.AuthResponse;
import edu.casas.budgetbuddy.dto.LoginRequest;
import edu.casas.budgetbuddy.entity.User;
import edu.casas.budgetbuddy.factory.ResponseFactory;
import edu.casas.budgetbuddy.service.UserService;
import org.springframework.stereotype.Component;

@Component
public class EmailPasswordAuthStrategy implements AuthStrategy {

    private final UserService userService;

    public EmailPasswordAuthStrategy(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseFactory.error("USER_NOT_FOUND", "User not found", "No account is registered for the provided email.");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseFactory.error("INVALID_PASSWORD", "Invalid password", "The supplied password does not match the saved password.");
        }

        return ResponseFactory.success("Login successful", user);
    }
}
