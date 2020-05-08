package com.h.pixeldroid

import android.app.Application
import androidx.preference.PreferenceManager
import com.h.pixeldroid.utils.ThemeUtils

class Pixeldroid: Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        ThemeUtils.setThemeFromPreferences(sharedPreferences, resources)
    }
}