package com.h.pixeldroid.settings

import android.content.Intent
import android.os.Bundle
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.BaseActivity
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