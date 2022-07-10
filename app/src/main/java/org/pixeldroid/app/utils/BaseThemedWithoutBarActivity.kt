package org.pixeldroid.app.utils

import android.os.Bundle

open class BaseThemedWithoutBarActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeNoActionBar())
        super.onCreate(savedInstanceState)
    }
}