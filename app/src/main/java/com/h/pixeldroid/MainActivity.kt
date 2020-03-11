package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button_login = findViewById<Button>(R.id.button_start_login)
        button_login.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent) }))

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent) }))

        val feedButton = findViewById<Button>(R.id.feedButton)
        feedButton.setOnClickListener((View.OnClickListener {
            startActivity(Intent(this, FeedActivity::class.java))
        }))
    }
}
