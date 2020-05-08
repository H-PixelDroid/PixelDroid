package com.h.pixeldroid.utils

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.h.pixeldroid.R

class ThemeUtils {
    companion object {
        /**
         * @brief Updates the application's theme depending on the given preferences and resources
         */
        fun setThemeFromPreferences(preferences: SharedPreferences, resources : Resources) {
            val themes = resources.getStringArray(R.array.theme_values)
            val theme = preferences.getString("theme", "default")
            Log.e("activeTheme", theme!!)
            //Set the theme
            when(theme) {
                //Default
                themes[0] -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                //Light
                themes[1] -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                //Dark
                themes[2] -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                else -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }
}