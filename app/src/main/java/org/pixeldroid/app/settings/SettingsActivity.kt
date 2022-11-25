package org.pixeldroid.app.settings

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.setThemeFromPreferences


class SettingsActivity : BaseThemedWithBarActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var restartMainOnExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.menu_settings)

        restartMainOnExit = intent.getBooleanExtra("restartMain", false)
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
            this
        )
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
            this
        )
    }

    override fun onBackPressed() {
        // If a setting (for example language or theme) was changed, the main activity should be
        // started without history so that the change is applied to the whole back stack
        if (restartMainOnExit) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            super.startActivity(intent)
        } else {
            super.onBackPressed()
        }
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "theme" -> {
                setThemeFromPreferences(sharedPreferences, resources)
                recreateWithRestartStatus()
            }
            "themeColor" -> {
                recreateWithRestartStatus()
            }
        }
    }

    /**
     * Mark main activity to be changed and recreate the current one
     */
    private fun recreateWithRestartStatus() {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val savedInstanceState = Bundle().apply {
            putBoolean("restartMain", true)
        }
        intent.putExtras(savedInstanceState)
        finish()
        super.startActivity(intent)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onDisplayPreferenceDialog(preference: Preference) {
            var dialogFragment: DialogFragment? = null
            if (preference is ColorPreference) {
                dialogFragment = ColorPreferenceDialog((preference as ColorPreference?)!!)
            } else if(preference.key == "language"){
                dialogFragment = LanguageSettingFragment()
            }
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, "settings_fragment")
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<ListPreference>("language")?.let {
                it.setSummaryProvider {
                    val locale = AppCompatDelegate.getApplicationLocales().get(0)
                    locale?.getDisplayName(locale) ?: getString(R.string.default_system)
                }
            }

            //Hide Notification setting for Android versions where it doesn't work
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                findPreference<Preference>("notification")
                    ?.let { preferenceScreen.removePreference(it) }
            }
        }
    }

}
class LanguageSettingFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val list: MutableList<String> = mutableListOf()
        resources.getXml(R.xml.locales_config).use {
            var eventType = it.eventType
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                when (eventType) {
                    XmlResourceParser.START_TAG -> {
                        if (it.name == "locale") {
                            list.add(it.getAttributeValue(0))
                        }
                    }
                }
                eventType = it.next()
            }
        }
        val locales = AppCompatDelegate.getApplicationLocales()
        val checkedItem: Int =
            if(locales.isEmpty) 0
            else {
                // For some reason we get a bit inconsistent language tags. This normalises it for
                // the currently used languages, but it might break in the future if we add some
                val index = list.indexOf(locales.get(0)?.toLanguageTag()?.lowercase()?.replace('_', '-'))
                // If found, we want to compensate for the first in the list being the default
                if(index == -1) -1
                else index + 1
            }

        return AlertDialog.Builder(requireContext()).apply {
            setIcon(R.drawable.translate_black_24dp)
            setTitle(R.string.language)
            setSingleChoiceItems((mutableListOf(getString(R.string.default_system)) + list.map {
                val appLocale = LocaleListCompat.forLanguageTags(it)
                appLocale.get(0)!!.getDisplayName(appLocale.get(0)!!)
            }).toTypedArray(), checkedItem) { dialog, which ->
                val languageTag = if(which in 1..list.size) list[which - 1] else null
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            }
            setNegativeButton(android.R.string.ok) { _, _ -> }
        }.create()
    }
}
