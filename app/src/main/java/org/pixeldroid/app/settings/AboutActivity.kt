package org.pixeldroid.app.settings

import android.content.Intent
import android.os.Bundle
import org.pixeldroid.app.BuildConfig
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityAboutBinding
import org.pixeldroid.app.utils.BaseThemedWithBarActivity

class AboutActivity : BaseThemedWithBarActivity() {
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