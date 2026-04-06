package edu.casas.budgetbuddy.dto;

public class AuthResponse {

    private final boolean success;
    private final String message;
    private final AuthData data;
    private final ApiError error;
    private final String timestamp;

    public AuthResponse(boolean success, String message, AuthData data, ApiError error, String timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public AuthData getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
