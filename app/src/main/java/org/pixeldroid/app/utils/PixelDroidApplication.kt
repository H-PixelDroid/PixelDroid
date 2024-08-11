package org.pixeldroid.app.utils

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import org.ligi.tracedroid.TraceDroid
import javax.inject.Inject

@HiltAndroidApp
class PixelDroidApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }
    override val workManagerConfiguration =
        Configuration.Builder()
            .setWorkerFactory(EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory())            .build()

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        setThemeFromPreferences(sharedPreferences, resources)

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}