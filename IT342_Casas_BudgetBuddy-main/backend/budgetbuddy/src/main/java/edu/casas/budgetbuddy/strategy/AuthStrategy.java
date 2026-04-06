package edu.casas.budgetbuddy.strategy;

import edu.casas.budgetbuddy.dto.AuthResponse;
import edu.casas.budgetbuddy.dto.LoginRequest;

public interface AuthStrategy {

    AuthResponse authenticate(LoginRequest request);
}
