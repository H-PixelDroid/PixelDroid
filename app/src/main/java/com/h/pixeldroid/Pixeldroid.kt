package com.h.pixeldroid

import android.app.Application
import androidx.preference.PreferenceManager
import com.h.pixeldroid.di.*
import com.h.pixeldroid.utils.ThemeUtils


class Pixeldroid: Application() {

    private lateinit var mApplicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        ThemeUtils.setThemeFromPreferences(sharedPreferences, resources)
        mApplicationComponent = DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .databaseModule(DatabaseModule(applicationContext))
            .aPIModule(APIModule())
            .build()
        mApplicationComponent.inject(this);
    }

    fun getAppComponent(): ApplicationComponent {
        return mApplicationComponent
    }
}