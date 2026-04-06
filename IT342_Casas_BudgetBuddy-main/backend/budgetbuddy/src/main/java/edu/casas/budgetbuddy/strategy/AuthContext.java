package edu.casas.budgetbuddy.strategy;

import edu.casas.budgetbuddy.dto.AuthResponse;
import edu.casas.budgetbuddy.dto.LoginRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    private final AuthStrategy authStrategy;

    public AuthContext(EmailPasswordAuthStrategy authStrategy) {
        this.authStrategy = authStrategy;
    }

    public AuthResponse authenticate(LoginRequest request) {
        return authStrategy.authenticate(request);
    }
}
