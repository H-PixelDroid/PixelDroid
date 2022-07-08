package org.pixeldroid.app.utils

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import org.pixeldroid.app.utils.di.*
import com.mikepenz.iconics.Iconics
import org.ligi.tracedroid.TraceDroid


class PixelDroidApplication: Application() {

    private lateinit var mApplicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        setThemeFromPreferences(sharedPreferences, resources)
        mApplicationComponent = DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .databaseModule(DatabaseModule(applicationContext))
            .aPIModule(APIModule())
            .build()
        mApplicationComponent.inject(this)

        //TODO put condition depending on setting
        if(false) DynamicColors.applyToActivitiesIfAvailable(this)
    }

    fun getAppComponent(): ApplicationComponent {
        return mApplicationComponent
    }
}