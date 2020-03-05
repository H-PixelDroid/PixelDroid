package com.h.pixeldroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homepage_button : ImageButton = findViewById(R.id.activity_main_home_btn)
        val search_button : ImageButton = findViewById(R.id.activity_main_search_btn)
        val camera_button : ImageButton = findViewById(R.id.activity_main_camera_btn)
        val favorite_button : ImageButton = findViewById(R.id.activity_main_favorite_btn)
        val account_button : ImageButton = findViewById(R.id.activity_main_account_btn)

        homepage_button.setOnClickListener(
            View.OnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }
        )
    }

}
