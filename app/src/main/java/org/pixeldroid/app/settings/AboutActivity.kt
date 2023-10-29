package org.pixeldroid.app.settings

import android.content.Intent
import android.os.Bundle
import org.pixeldroid.app.BuildConfig
import org.pixeldroid.app.databinding.ActivityAboutBinding
import org.pixeldroid.app.utils.ThemedActivity

class AboutActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.aboutVersionNumber.text = BuildConfig.VERSION_NAME
        binding.licensesButton.setOnClickListener{
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }
    }
}