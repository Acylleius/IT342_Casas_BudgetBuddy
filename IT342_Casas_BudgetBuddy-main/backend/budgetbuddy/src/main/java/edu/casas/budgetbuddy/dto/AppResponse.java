package edu.casas.budgetbuddy.dto;

import java.time.LocalDateTime;

public class AppResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ApiError error;
    private final String timestamp;

    public AppResponse(boolean success, String message, T data, ApiError error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = LocalDateTime.now().toString();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
