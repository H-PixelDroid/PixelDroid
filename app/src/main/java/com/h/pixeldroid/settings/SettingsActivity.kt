package com.h.pixeldroid.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.h.pixeldroid.MainActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.setThemeFromPreferences

class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var restartMainOnExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        super.startActivity(intent)
        finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

}