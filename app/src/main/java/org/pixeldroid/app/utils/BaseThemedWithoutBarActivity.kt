package org.pixeldroid.app.utils

import android.os.Bundle

open class BaseThemedWithoutBarActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme when we chose one
        themeNoActionBar()?.let { setTheme(it) }
        super.onCreate(savedInstanceState)
    }
}