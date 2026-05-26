package com.budgetbuddy.mobile.util

import android.content.Context
import com.budgetbuddy.mobile.model.User

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("budgetbuddy_session", Context.MODE_PRIVATE)

    fun saveSession(token: String, user: User, refreshToken: String? = null) {
        prefs.edit()
            .putString("token", token)
            .putString("refreshToken", refreshToken)
            .putString("email", user.email)
            .putString("firstname", user.firstname)
            .putString("lastname", user.lastname)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun getRefreshToken(): String? = prefs.getString("refreshToken", null)

    fun getDisplayName(): String {
        val firstname = prefs.getString("firstname", null).orEmpty()
        val lastname = prefs.getString("lastname", null).orEmpty()
        return "$firstname $lastname".trim().ifBlank { prefs.getString("email", "BudgetBuddy User").orEmpty() }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
