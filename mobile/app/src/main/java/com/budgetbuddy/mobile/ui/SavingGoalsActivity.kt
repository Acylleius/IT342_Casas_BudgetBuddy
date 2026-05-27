package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.SavingGoal
import com.budgetbuddy.mobile.model.SavingGoalRequest
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SavingGoalsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saving_goals)
        findViewById<Button>(R.id.saveGoalButton).setOnClickListener { createGoal() }
        loadGoals()
    }

    private fun createGoal() {
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) return
        val title = findViewById<EditText>(R.id.goalTitleField).text.toString()
        val target = findViewById<EditText>(R.id.goalTargetField).text.toString().toDoubleOrNull()
        val current = findViewById<EditText>(R.id.goalCurrentField).text.toString().toDoubleOrNull() ?: 0.0
        if (title.isBlank() || target == null) {
            Toast.makeText(this, "Enter goal title and target", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.instance.createSavingGoal("Bearer $token", SavingGoalRequest(title, target, current))
            .enqueue(object : Callback<ApiResponse<SavingGoal>> {
                override fun onResponse(call: Call<ApiResponse<SavingGoal>>, response: Response<ApiResponse<SavingGoal>>) {
                    Toast.makeText(this@SavingGoalsActivity, "Goal saved", Toast.LENGTH_SHORT).show()
                    loadGoals()
                }

                override fun onFailure(call: Call<ApiResponse<SavingGoal>>, t: Throwable) {
                    Toast.makeText(this@SavingGoalsActivity, "Goal error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGoals() {
        val list = findViewById<TextView>(R.id.goalList)
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) {
            list.text = "Please log in again."
            return
        }
        RetrofitClient.instance.savingGoals("Bearer $token").enqueue(object : Callback<ApiResponse<List<SavingGoal>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<SavingGoal>>>,
                response: Response<ApiResponse<List<SavingGoal>>>
            ) {
                val goals = response.body()?.data.orEmpty()
                list.text = if (goals.isEmpty()) {
                    "No saving goals yet."
                } else {
                    goals.joinToString("\n\n") {
                        "${it.title}\n${it.status} - ${it.percentageUsed}% saved\nPHP ${it.currentAmount} of PHP ${it.targetAmount}\nRemaining PHP ${it.remainingAmount}"
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<SavingGoal>>>, t: Throwable) {
                list.text = "Goal load error: ${t.message}"
            }
        })
    }
}
