package org.pixeldroid.app.settings

import android.os.Bundle
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityLicensesBinding
import org.pixeldroid.app.utils.BaseActivity

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