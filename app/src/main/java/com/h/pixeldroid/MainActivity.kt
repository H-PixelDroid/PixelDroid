package com.h.pixeldroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button_start_login)
        button.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent) }))
    }

}
