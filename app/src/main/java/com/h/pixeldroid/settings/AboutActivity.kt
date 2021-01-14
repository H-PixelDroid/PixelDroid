package com.h.pixeldroid.settings

import android.content.Intent
import android.os.Bundle
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityAboutBinding
import com.h.pixeldroid.utils.BaseActivity

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.about_pixeldroid)

        binding.aboutVersionNumber.text = BuildConfig.VERSION_NAME
        binding.licensesButton.setOnClickListener{
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }
    }
}