package com.h.pixeldroid

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.*


open class BaseActivity : AppCompatActivity() {

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

    @SuppressWarnings("deprecation")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

}