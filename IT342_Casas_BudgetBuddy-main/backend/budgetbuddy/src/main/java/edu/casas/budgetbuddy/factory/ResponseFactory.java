package edu.casas.budgetbuddy.factory;

import edu.casas.budgetbuddy.dto.ApiError;
import edu.casas.budgetbuddy.dto.AuthData;
import edu.casas.budgetbuddy.dto.AuthResponse;
import edu.casas.budgetbuddy.dto.UserDto;
import edu.casas.budgetbuddy.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static AuthResponse success(String message, User user) {
        AuthData authData = null;

        if (user != null) {
            authData = new AuthData(
                    UserDto.from(user),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
            );
        }

        return new AuthResponse(true, message, authData, null, LocalDateTime.now().toString());
    }

    public static AuthResponse error(String code, String message, String details) {
        return new AuthResponse(
                false,
                message,
                null,
                new ApiError(code, message, details),
                LocalDateTime.now().toString()
        );
    }
}
