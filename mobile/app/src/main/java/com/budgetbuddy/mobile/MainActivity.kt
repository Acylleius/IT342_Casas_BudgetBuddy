package com.budgetbuddy.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.ui.DashboardActivity
import com.budgetbuddy.mobile.ui.LoginActivity
import com.budgetbuddy.mobile.util.SessionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = if (SessionManager(this).getToken().isNullOrBlank()) {
            LoginActivity::class.java
        } else {
            DashboardActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}
