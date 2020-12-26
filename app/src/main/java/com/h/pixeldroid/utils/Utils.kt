package com.h.pixeldroid.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.h.pixeldroid.R

fun hasInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetwork != null
}

fun normalizeDomain(domain: String): String {
    return "https://" + domain
            .replace("http://", "")
            .replace("https://", "")
            .trim(Char::isWhitespace)
}


/**
 * @brief Updates the application's theme depending on the given preferences and resources
 */
fun setThemeFromPreferences(preferences: SharedPreferences, resources : Resources) {
    val themes = resources.getStringArray(R.array.theme_values)
    //Set the theme
    when(preferences.getString("theme", "")) {
        //Light
        themes[1] -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        //Dark
        themes[2] -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}
