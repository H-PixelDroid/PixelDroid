package com.h.pixeldroid.settings

import android.os.Bundle
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityLicensesBinding
import com.h.pixeldroid.utils.BaseActivity

class LicenseActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLicensesBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.dependencies_licenses)

        binding.webview.loadUrl("file:///android_asset/licenses.html")
    }
}