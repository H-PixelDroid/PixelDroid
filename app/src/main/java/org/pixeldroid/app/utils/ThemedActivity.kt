package org.pixeldroid.app.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class ThemedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme when we chose one
        themeNoActionBar()?.let { setTheme(it) }
        super.onCreate(savedInstanceState)
    }
}