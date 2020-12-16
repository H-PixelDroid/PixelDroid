package com.h.pixeldroid

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_licenses.*


class LicenseActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.dependencies_licenses)
        webview.loadUrl("file:///android_asset/licenses.html")
    }
}