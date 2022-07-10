package org.pixeldroid.app.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
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
            "language" -> {
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

            //Hide Notification setting for Android versions where it doesn't work
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                preferenceManager.findPreference<Preference>("notification")
                    ?.let { preferenceScreen.removePreference(it) }
            }
        }
    }

}