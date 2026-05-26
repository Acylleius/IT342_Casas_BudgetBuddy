package com.budgetbuddy.mobile.ui

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.budgetbuddy.mobile.R
import com.budgetbuddy.mobile.util.SessionManager

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.text = "Welcome, ${SessionManager(this).getDisplayName()}!"

        findViewById<Button>(R.id.groupsButton).setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
        }
        findViewById<Button>(R.id.inboxButton).setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
        }
    }
}
