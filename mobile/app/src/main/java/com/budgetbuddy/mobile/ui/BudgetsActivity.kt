package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.Budget
import com.budgetbuddy.mobile.model.BudgetRequest
import com.budgetbuddy.mobile.model.BudgetTracking
import com.budgetbuddy.mobile.util.Categories
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BudgetsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budgets)

        val spinner = findViewById<Spinner>(R.id.budgetPeriodSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("WEEKLY", "MONTHLY"))
        findViewById<Spinner>(R.id.budgetCategorySpinner).adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Categories.values)

        findViewById<Button>(R.id.saveBudgetButton).setOnClickListener { createBudget() }
        loadBudgets()
    }

    private fun createBudget() {
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) return
        val name = findViewById<EditText>(R.id.budgetNameField).text.toString()
        val amount = findViewById<EditText>(R.id.budgetLimitField).text.toString().toDoubleOrNull()
        if (name.isBlank() || amount == null) {
            Toast.makeText(this, "Enter budget name and amount", Toast.LENGTH_SHORT).show()
            return
        }
        val period = findViewById<Spinner>(R.id.budgetPeriodSpinner).selectedItem.toString()
        val category = findViewById<Spinner>(R.id.budgetCategorySpinner).selectedItem.toString()
        RetrofitClient.instance.createBudget("Bearer $token", BudgetRequest(name, amount, period, category))
            .enqueue(object : Callback<ApiResponse<Budget>> {
                override fun onResponse(call: Call<ApiResponse<Budget>>, response: Response<ApiResponse<Budget>>) {
                    Toast.makeText(this@BudgetsActivity, "Budget saved", Toast.LENGTH_SHORT).show()
                    loadBudgets()
                }

                override fun onFailure(call: Call<ApiResponse<Budget>>, t: Throwable) {
                    Toast.makeText(this@BudgetsActivity, "Budget error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadBudgets() {
        val list = findViewById<TextView>(R.id.budgetList)
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) {
            list.text = "Please log in again."
            return
        }
        RetrofitClient.instance.budgetTracking("Bearer $token").enqueue(object : Callback<ApiResponse<List<BudgetTracking>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<BudgetTracking>>>,
                response: Response<ApiResponse<List<BudgetTracking>>>
            ) {
                val items = response.body()?.data.orEmpty()
                list.text = if (items.isEmpty()) {
                    "No budgets yet."
                } else {
                    items.joinToString("\n\n") {
                        val budget = it.budget
                        "${budget.name} (${budget.period}, ${Categories.label(budget.category)})\n${budget.status} - ${budget.percentageUsed}% used\nSpent PHP ${budget.spentAmount} of PHP ${budget.limitAmount}\nRemaining PHP ${budget.remainingAmount}"
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<BudgetTracking>>>, t: Throwable) {
                list.text = "Budget load error: ${t.message}"
            }
        })
    }
}
