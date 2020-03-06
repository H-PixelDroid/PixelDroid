package com.h.pixeldroid.settings.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.h.pixeldroid.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)
    }

}
