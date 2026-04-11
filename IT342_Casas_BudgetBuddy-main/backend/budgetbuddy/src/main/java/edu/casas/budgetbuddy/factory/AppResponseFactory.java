package edu.casas.budgetbuddy.factory;

import edu.casas.budgetbuddy.dto.ApiError;
import edu.casas.budgetbuddy.dto.AppResponse;

public final class AppResponseFactory {

    private AppResponseFactory() {
    }

    public static <T> AppResponse<T> success(String message, T data) {
        return new AppResponse<>(true, message, data, null);
    }

    public static <T> AppResponse<T> error(String code, String message, String details) {
        return new AppResponse<>(false, message, null, new ApiError(code, message, details));
    }
}
