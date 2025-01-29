package org.pixeldroid.app.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.SettingsBinding
import org.pixeldroid.app.main.MainActivity
import org.pixeldroid.app.settings.TutorialSettingsDialog.Companion.START_TUTORIAL
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.CommonWrapper
import org.pixeldroid.app.utils.setThemeFromPreferences


@AndroidEntryPoint
class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var restartMainOnExit = false
    private lateinit var binding: SettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment(), "topsettingsfragment")
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val showTutorial = intent.getBooleanExtra(START_TUTORIAL, false)

        if(showTutorial){
            lifecycleScope.launch {
                var target =
                    (supportFragmentManager.findFragmentByTag("topsettingsfragment") as? SettingsFragment)?.scrollToArrangeTabs(10)
                while (target == null) {
                    target = (supportFragmentManager.findFragmentByTag("topsettingsfragment") as? SettingsFragment)?.scrollToArrangeTabs(10)
                    delay(100)
                }
                target.performClick()
            }
        }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
            // Handle the back button event
            // If a setting (for example language or theme) was changed, the main activity should be
            // started without history so that the change is applied to the whole back stack
            //TODO restore behaviour without true here, so that MainActivity is not destroyed when not necessary
            // The true is a "temporary" (lol) fix so that tab changes are always taken into account
            // Also, consider making the up button (arrow in action bar) also take this codepath!
            // It recreates the activity by default
            if (true || restartMainOnExit) {
                val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                super@SettingsActivity.startActivity(intent)
            } else {
                finish()
            }
        }

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            when (key) {
                "theme" -> {
                    setThemeFromPreferences(it, resources)
                    recreateWithRestartStatus()
                }

                "themeColor" -> {
                    recreateWithRestartStatus()
                }

                "always_show_nsfw" -> {
                    lifecycleScope.launch {
                        val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                        try {
                            // Get old settings and modify just the nsfw one
                            val settings = api.getSettings().let { settings ->
                                settings.common.copy(
                                    media = settings.common.media.copy(
                                        always_show_cw = sharedPreferences.getBoolean(key, settings.common.media.always_show_cw)
                                    )
                                )
                            }
                            api.setSettings(CommonWrapper(settings))
                        } catch (e: Exception) {
                            Log.e("Pixelfed API settings", e.toString())
                        }
                    }
                }

                else -> {}
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

    fun customTabsTutorial(){
        lifecycleScope.launch {
            var target =
                (supportFragmentManager.findFragmentByTag("topsettingsfragment") as? SettingsFragment)?.scrollToArrangeTabs(5)
            while (target == null) {
                target = (supportFragmentManager.findFragmentByTag("topsettingsfragment") as? SettingsFragment)?.scrollToArrangeTabs(5)
                delay(100)
            }
            TapTargetView.showFor(
                this@SettingsActivity,
                TapTarget.forView(target, getString(R.string.arrange_tabs_tutorial_title))
                    .transparentTarget(true)
                    .targetRadius(60),  // Specify the target radius (in dp)
                object : TapTargetView.Listener() {
                    // The listener can listen for regular clicks, long clicks or cancels
                    override fun onTargetClick(view: TapTargetView?) {
                        super.onTargetClick(view) // This call is optional
                        // Perform action for the current target
                        val dialogFragment = ArrangeTabsFragment().apply { showTutorial = true }
                        dialogFragment.setTargetFragment(
                            (supportFragmentManager.findFragmentByTag("topsettingsfragment") as? SettingsFragment),
                            0
                        )
                        dialogFragment.show(supportFragmentManager, "settings_fragment")
                    }
                })
        }

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onDisplayPreferenceDialog(preference: Preference) {
            var dialogFragment: DialogFragment? = null
            if (preference is ColorPreference) {
                dialogFragment = ColorPreferenceDialog((preference as ColorPreference?)!!)
            } else if(preference.key == "language"){
                dialogFragment = LanguageSettingFragment()
            } else if (preference.key == "arrange_tabs") {
                dialogFragment = ArrangeTabsFragment()
            } else if (preference.key == "tutorial") {
                dialogFragment = TutorialSettingsDialog()
            }
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, "settings_fragment")
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }
        fun scrollToArrangeTabs(position: Int): View? {
            //Hardcoded positions because it's too annoying to find!

            if (listView != null && position != -1) {
                listView.post {
                    listView.smoothScrollToPosition(position)
                }
            }
            return listView.findViewHolderForAdapterPosition(position)?.itemView
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<ListPreference>("language")?.let {
                it.setSummaryProvider {
                    val locale = AppCompatDelegate.getApplicationLocales().get(0)
                    locale?.getDisplayName(locale) ?: getString(R.string.default_system)
                }
            }
            findPreference<CheckBoxPreference>("always_show_nsfw")?.let {
                lifecycleScope.launch {
                    val api: PixelfedAPI = (requireActivity() as SettingsActivity).apiHolder.api ?: (requireActivity() as SettingsActivity).apiHolder.setToCurrentUser()

                    try {
                        val show = api.getSettings().common.media.always_show_cw
                        it.isChecked = show
                    } catch (_: Exception){}
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