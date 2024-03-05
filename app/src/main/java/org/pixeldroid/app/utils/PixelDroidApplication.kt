package org.pixeldroid.app.utils

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import org.ligi.tracedroid.TraceDroid

@HiltAndroidApp
class PixelDroidApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        setThemeFromPreferences(sharedPreferences, resources)

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}