package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.BudgetTracking
import com.budgetbuddy.mobile.model.GroupHistoryItem
import com.budgetbuddy.mobile.model.GroupTransaction
import com.budgetbuddy.mobile.model.SavingGoal
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)
        loadGroupDetail(intent.getLongExtra("groupId", 0L))
    }

    private fun loadGroupDetail(groupId: Long) {
        val list = findViewById<TextView>(R.id.groupDetailList)
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank() || groupId == 0L) {
            list.text = "Please open a valid group."
            return
        }
        val auth = "Bearer $token"
        RetrofitClient.instance.groupTransactions(auth, groupId)
            .enqueue(object : Callback<ApiResponse<List<GroupTransaction>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<GroupTransaction>>>,
                    response: Response<ApiResponse<List<GroupTransaction>>>
                ) {
                    val transactions = response.body()?.data.orEmpty()
                    RetrofitClient.instance.groupHistory(auth, groupId)
                        .enqueue(object : Callback<ApiResponse<List<GroupHistoryItem>>> {
                            override fun onResponse(
                                call: Call<ApiResponse<List<GroupHistoryItem>>>,
                                response: Response<ApiResponse<List<GroupHistoryItem>>>
                            ) {
                                val history = response.body()?.data.orEmpty()
                                loadBudgetsAndGoals(auth, groupId, transactions, history, list)
                            }

                            override fun onFailure(call: Call<ApiResponse<List<GroupHistoryItem>>>, t: Throwable) {
                                list.text = "History error: ${t.message}"
                            }
                        })
                }

                override fun onFailure(call: Call<ApiResponse<List<GroupTransaction>>>, t: Throwable) {
                    Toast.makeText(this@GroupDetailActivity, "Group error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadBudgetsAndGoals(
        auth: String,
        groupId: Long,
        transactions: List<GroupTransaction>,
        history: List<GroupHistoryItem>,
        list: TextView
    ) {
        RetrofitClient.instance.groupBudgetTracking(auth, groupId)
            .enqueue(object : Callback<ApiResponse<List<BudgetTracking>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<BudgetTracking>>>,
                    response: Response<ApiResponse<List<BudgetTracking>>>
                ) {
                    val budgets = response.body()?.data.orEmpty()
                    RetrofitClient.instance.groupSavingGoals(auth, groupId)
                        .enqueue(object : Callback<ApiResponse<List<SavingGoal>>> {
                            override fun onResponse(
                                call: Call<ApiResponse<List<SavingGoal>>>,
                                response: Response<ApiResponse<List<SavingGoal>>>
                            ) {
                                val goals = response.body()?.data.orEmpty()
                                list.text = buildString {
                                    appendLine("Transactions")
                                    appendLine(transactions.joinToString("\n") {
                                        "${it.actorUsername} ${it.type} ${it.category}: PHP ${it.amount}"
                                    }.ifBlank { "No group transactions yet." })
                                    appendLine()
                                    appendLine("Budgets")
                                    appendLine(budgets.joinToString("\n") {
                                        "${it.budget.name}: ${it.budget.status}, ${it.budget.percentageUsed}% used"
                                    }.ifBlank { "No group budgets yet." })
                                    appendLine()
                                    appendLine("Saving Goals")
                                    appendLine(goals.joinToString("\n") {
                                        "${it.title}: ${it.status}, PHP ${it.currentAmount} / PHP ${it.targetAmount}"
                                    }.ifBlank { "No group saving goals yet." })
                                    appendLine()
                                    appendLine("History")
                                    append(history.joinToString("\n") { "${it.actorUsername}: ${it.description}" }
                                        .ifBlank { "No group history yet." })
                                }
                            }

                            override fun onFailure(call: Call<ApiResponse<List<SavingGoal>>>, t: Throwable) {
                                list.text = "Group saving goals error: ${t.message}"
                            }
                        })
                }

                override fun onFailure(call: Call<ApiResponse<List<BudgetTracking>>>, t: Throwable) {
                    list.text = "Group budgets error: ${t.message}"
                }
            })
    }
}
