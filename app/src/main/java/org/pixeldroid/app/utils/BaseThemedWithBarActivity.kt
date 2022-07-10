package org.pixeldroid.app.utils

import android.os.Bundle

open class BaseThemedWithBarActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeActionBar())
        super.onCreate(savedInstanceState)
    }
}