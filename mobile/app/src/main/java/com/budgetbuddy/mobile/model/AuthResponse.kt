package com.budgetbuddy.mobile.model

data class AuthResponse(
    val success: Boolean = false,
    val data: AuthData? = null,
    val message: String? = null
)

data class AuthData(
    val user: User,
    val token: String,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

data class User(
    val id: Long,
    val email: String,
    val firstname: String,
    val lastname: String,
    val role: String,
    val authProvider: String? = null
)
