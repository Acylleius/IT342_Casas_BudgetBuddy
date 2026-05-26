package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.RegisterRequest
import com.budgetbuddy.mobile.model.AuthResponse
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val firstnameField = findViewById<EditText>(R.id.firstnameField)
        val lastnameField = findViewById<EditText>(R.id.lastnameField)
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val sessionManager = SessionManager(this)

        loginButton.setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            val request = RegisterRequest(
                firstnameField.text.toString(),
                lastnameField.text.toString(),
                emailField.text.toString(),
                passwordField.text.toString()
            )

            RetrofitClient.instance.register(request).enqueue(object: Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    val authData = response.body()?.data
                    if (response.isSuccessful && response.body()?.success == true && authData != null) {
                        sessionManager.saveSession(authData.token, authData.user)
                        Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val message = response.body()?.message ?: "Registration failed: ${response.code()}"
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
