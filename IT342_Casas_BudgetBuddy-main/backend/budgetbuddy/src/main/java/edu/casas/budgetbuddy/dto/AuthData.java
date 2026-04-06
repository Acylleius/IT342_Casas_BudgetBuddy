package edu.casas.budgetbuddy.dto;

public class AuthData {

    private final UserDto user;
    private final String accessToken;
    private final String refreshToken;

    public AuthData(UserDto user, String accessToken, String refreshToken) {
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public UserDto getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
