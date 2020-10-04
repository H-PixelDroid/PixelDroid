package com.h.pixeldroid

import android.app.Application
import androidx.preference.PreferenceManager
import com.h.pixeldroid.di.*
import com.h.pixeldroid.utils.ThemeUtils
import com.mikepenz.iconics.Iconics
import org.ligi.tracedroid.TraceDroid


class Pixeldroid: Application() {

    private lateinit var mApplicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        TraceDroid.init(this)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        ThemeUtils.setThemeFromPreferences(sharedPreferences, resources)
        mApplicationComponent = DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .databaseModule(DatabaseModule(applicationContext))
            .aPIModule(APIModule())
            .build()
        mApplicationComponent.inject(this)

        Iconics.init(applicationContext)
    }

    fun getAppComponent(): ApplicationComponent {
        return mApplicationComponent
    }
}