package org.pixeldroid.app.utils

import android.os.Bundle

open class BaseThemedWithBarActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme when we chose one
        themeActionBar()?.let { setTheme(it) }
        super.onCreate(savedInstanceState)
    }
}