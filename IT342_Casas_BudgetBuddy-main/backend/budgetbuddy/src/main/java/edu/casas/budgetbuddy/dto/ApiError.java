package edu.casas.budgetbuddy.dto;

public class ApiError {

    private final String code;
    private final String message;
    private final String details;

    public ApiError(String code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}
