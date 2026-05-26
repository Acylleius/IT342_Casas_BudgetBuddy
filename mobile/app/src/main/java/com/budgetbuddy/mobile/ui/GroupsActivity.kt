package com.budgetbuddy.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.Group
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupsActivity : AppCompatActivity() {
    private var groups: List<Group> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)
        findViewById<TextView>(R.id.groupsList).setOnClickListener {
            groups.firstOrNull()?.let { group ->
                startActivity(Intent(this, GroupDetailActivity::class.java).putExtra("groupId", group.id))
            }
        }
        loadGroups()
    }

    private fun loadGroups() {
        val list = findViewById<TextView>(R.id.groupsList)
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) {
            list.text = "Please log in again."
            return
        }
        RetrofitClient.instance.groups("Bearer $token").enqueue(object : Callback<ApiResponse<List<Group>>> {
            override fun onResponse(call: Call<ApiResponse<List<Group>>>, response: Response<ApiResponse<List<Group>>>) {
                groups = response.body()?.data.orEmpty()
                list.text = if (groups.isEmpty()) {
                    "No groups yet. Create one on the web or through the API."
                } else {
                    groups.joinToString("\n\n") { "${it.name}\n${it.description.orEmpty()}\nTap to open first group." }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Group>>>, t: Throwable) {
                Toast.makeText(this@GroupsActivity, "Groups error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
