package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.GroupHistoryItem
import com.budgetbuddy.mobile.model.GroupTransaction
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
                                list.text = buildString {
                                    appendLine("Transactions")
                                    appendLine(transactions.ifEmpty { emptyList() }.joinToString("\n") {
                                        "${it.actorUsername} ${it.type} ${it.category}: PHP ${it.amount}"
                                    }.ifBlank { "No group transactions yet." })
                                    appendLine()
                                    appendLine("History")
                                    append(history.joinToString("\n") { "${it.actorUsername}: ${it.description}" }
                                        .ifBlank { "No group history yet." })
                                }
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
}
