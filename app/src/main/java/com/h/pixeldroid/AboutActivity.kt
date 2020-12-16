package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.about_pixeldroid)

        aboutVersionNumber.text = BuildConfig.VERSION_NAME
        licensesButton.setOnClickListener{
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }
    }
}