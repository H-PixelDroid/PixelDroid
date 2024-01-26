package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.SettingsBinding
import org.pixeldroid.app.utils.setThemeFromPreferences
import org.pixeldroid.common.ThemedActivity


class SettingsActivity : ThemedActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var restartMainOnExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */) {
            // Handle the back button event
            // If a setting (for example language or theme) was changed, the main activity should be
            // started without history so that the change is applied to the whole back stack
            if (restartMainOnExit) {
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
            } else if (preference.key == "arrange_tabs") {
                dialogFragment = ArrangeTabsFragment()
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
        // IDE doesn't find it, but compiling works apparently?
        resources.getXml(R.xml._generated_res_locale_config).use {
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

        return MaterialAlertDialogBuilder(requireContext()).apply {
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

class ArrangeTabsFragment: DialogFragment() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater: LayoutInflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_tabs_arrange, null)

        // TODO: Get values from preferences instead of hard-coding them
        val list: List<String> = listOf("Home", "Search", "Create", "Updates", "Public")
        val map: MutableList<Pair<String, Boolean>> = list.zip(List(list.size){true}.toTypedArray()).toMutableList()

        val listFeed: RecyclerView = dialogView.findViewById(R.id.tabs)
        val listAdapter = ListViewAdapter(map)
        listFeed.adapter = listAdapter
        listFeed.layoutManager = LinearLayoutManager(requireActivity())
        val callback = object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                listAdapter.onItemMove(source.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Do nothing for now, all items should remain in the list
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(listFeed)

        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setIcon(R.drawable.outline_bottom_navigation)
            setTitle(R.string.arrange_tabs_summary)
            setView(dialogView)
            setNegativeButton(android.R.string.cancel) { _, _ -> }
            setPositiveButton(android.R.string.ok) { _, _ ->
                // TODO: Save values into preferences instead of doing nothing
            }
        }.create()

        return dialog
    }

    inner class ListViewAdapter(private val tabsChecked: MutableList<Pair<String, Boolean>>):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = FrameLayout.inflate(context, R.layout.layout_tab, null)

            // Make sure the layout occupies full width
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            return object: RecyclerView.ViewHolder(view) {}
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val textView: MaterialButton = holder.itemView.findViewById(R.id.textView)
            val checkBox: MaterialCheckBox = holder.itemView.findViewById(R.id.checkBox)
            val dragHandle: ImageView = holder.itemView.findViewById(R.id.dragHandle)

            // Set content of each entry
            textView.text = tabsChecked[position].first
            checkBox.isChecked = tabsChecked[position].second

            // Also interact with checkbox when button is clicked
            textView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
                tabsChecked[position] = Pair(tabsChecked[position].first, !tabsChecked[position].second)

                // Disable OK button when no tab is selected
                (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                    tabsChecked.any { (_, v) -> v }
            }

            // Also highlight button when checkbox is clicked
            checkBox.setOnTouchListener { _, motionEvent ->
                textView.dispatchTouchEvent(motionEvent)
            }

            // Do not highlight the button when the drag handle is touched
            dragHandle.setOnTouchListener { _, _ -> true }
        }

        override fun getItemCount(): Int {
            return tabsChecked.size
        }

        fun onItemMove(from: Int, to: Int) {
            val previous = tabsChecked.removeAt(from)
            tabsChecked.add(if (to > from) to - 1 else to, previous)
            notifyItemMoved(from, to)
        }
    }
}