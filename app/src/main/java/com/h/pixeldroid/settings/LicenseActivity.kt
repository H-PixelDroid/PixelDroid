package com.h.pixeldroid.settings

import android.os.Bundle
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.BaseActivity
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