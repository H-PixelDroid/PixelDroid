package com.h.pixeldroid.utils

import android.app.Application
import androidx.preference.PreferenceManager
import com.h.pixeldroid.utils.di.*
import com.mikepenz.iconics.Iconics
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist


class PixelDroidApplication: Application() {

    private lateinit var mApplicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)
        sendTraceDroidStackTracesIfExist("contact@pixeldroid.org", this)

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

        Iconics.init(applicationContext)
    }

    fun getAppComponent(): ApplicationComponent {
        return mApplicationComponent
    }
}