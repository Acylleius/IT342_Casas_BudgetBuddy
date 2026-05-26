package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.data.RetrofitClient
import com.budgetbuddy.mobile.model.ApiResponse
import com.budgetbuddy.mobile.model.InboxNotification
import com.budgetbuddy.mobile.util.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InboxActivity : AppCompatActivity() {
    private var firstInvitationId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        findViewById<Button>(R.id.acceptInviteButton).setOnClickListener {
            respondToFirstInvite(true)
        }
        findViewById<Button>(R.id.declineInviteButton).setOnClickListener {
            respondToFirstInvite(false)
        }
        loadInbox()
    }

    private fun loadInbox() {
        val list = findViewById<TextView>(R.id.inboxList)
        val token = SessionManager(this).getToken()
        if (token.isNullOrBlank()) {
            list.text = "Please log in again."
            return
        }
        RetrofitClient.instance.inbox("Bearer $token").enqueue(object : Callback<ApiResponse<List<InboxNotification>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<InboxNotification>>>,
                response: Response<ApiResponse<List<InboxNotification>>>
            ) {
                val items = response.body()?.data.orEmpty()
                firstInvitationId = items.firstOrNull { it.type == "GROUP_INVITE" && it.invitationId != null }?.invitationId
                list.text = if (items.isEmpty()) {
                    "No inbox messages yet."
                } else {
                    items.joinToString("\n\n") {
                        val state = if (it.isRead) "Read" else "Unread"
                        val inviteState = it.invitationStatus?.let { status -> " ($status)" }.orEmpty()
                        "$state$inviteState\n${it.title}\n${it.message}\n${it.createdAt}"
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<InboxNotification>>>, t: Throwable) {
                Toast.makeText(this@InboxActivity, "Inbox error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun respondToFirstInvite(accept: Boolean) {
        val invitationId = firstInvitationId
        val token = SessionManager(this).getToken()
        if (invitationId == null || token.isNullOrBlank()) {
            Toast.makeText(this, "No pending invitation found", Toast.LENGTH_SHORT).show()
            return
        }
        val call = if (accept) {
            RetrofitClient.instance.acceptInvitation("Bearer $token", invitationId)
        } else {
            RetrofitClient.instance.declineInvitation("Bearer $token", invitationId)
        }
        call.enqueue(object : Callback<ApiResponse<com.budgetbuddy.mobile.model.Invitation>> {
            override fun onResponse(
                call: Call<ApiResponse<com.budgetbuddy.mobile.model.Invitation>>,
                response: Response<ApiResponse<com.budgetbuddy.mobile.model.Invitation>>
            ) {
                Toast.makeText(this@InboxActivity, if (accept) "Invitation accepted" else "Invitation declined", Toast.LENGTH_SHORT).show()
                loadInbox()
            }

            override fun onFailure(call: Call<ApiResponse<com.budgetbuddy.mobile.model.Invitation>>, t: Throwable) {
                Toast.makeText(this@InboxActivity, "Invite error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
