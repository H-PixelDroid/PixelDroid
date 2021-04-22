package org.pixeldroid.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import java.util.*
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (this.application as PixelDroidApplication).getAppComponent().inject(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateBaseContextLocale(base))
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val language = PreferenceManager.getDefaultSharedPreferences(context).getString("language", "default") ?: "default"
        if(language == "default"){
            return context
        }
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else updateResourcesLocaleLegacy(context, locale)
    }

    private fun updateResourcesLocale(context: Context, locale: Locale): Context =
        context.createConfigurationContext(
                Configuration(context.resources.configuration)
                        .apply { setLocale(locale) }
        )

    @Suppress("DEPRECATION")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

}