package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.SettingsBinding
import org.pixeldroid.app.utils.Tab
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity
import org.pixeldroid.app.utils.loadDbMenuTabs
import org.pixeldroid.app.utils.loadDefaultMenuTabs
import org.pixeldroid.app.utils.setThemeFromPreferences
import org.pixeldroid.common.ThemedActivity
import javax.inject.Inject


@AndroidEntryPoint
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

@AndroidEntryPoint
class ArrangeTabsFragment: DialogFragment() {

    @Inject
    lateinit var db: AppDatabase

    private val model: ArrangeTabsViewModel by viewModels { ArrangeTabsViewModelFactory(requireContext(), db) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater: LayoutInflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_tabs_arrange, null)

        model.initTabsChecked(dialogView)

        val listFeed: RecyclerView = dialogView.findViewById(R.id.tabs)
        val listAdapter = ListViewAdapter(model)
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
                // Do nothing, all items should remain in the list
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
                // Save values into preferences
                val tabsChecked = listAdapter.model.uiState.value.tabsChecked.toList()
                val tabsDbEntity = tabsChecked.mapIndexed { index, (tab, checked) ->
                    TabsDatabaseEntity(index, db.userDao().getActiveUser()!!.user_id, db.instanceDao().getActiveInstance().uri, tab.name, checked)
                }
                lifecycleScope.launch {
                    db.tabsDao().clearAndRefill(tabsDbEntity, model.uiState.value.userId, model.uiState.value.instanceUri)
                }
            }
        }.create()

        return dialog
    }

    inner class ListViewAdapter(val model: ArrangeTabsViewModel):
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
            textView.text = model.uiState.value.tabsChecked[position].first.toLanguageString(requireContext())
            checkBox.isChecked = model.uiState.value.tabsChecked[position].second

            // Also interact with checkbox when button is clicked
            textView.setOnClickListener {
                val isCheckedNew = !model.uiState.value.tabsChecked[position].second
                model.tabsCheckReplace(position, Pair(model.uiState.value.tabsChecked[position].first, isCheckedNew))
                checkBox.isChecked = isCheckedNew

                // Disable OK button when no tab is selected or when strictly more than 5 tabs are selected
                val maxItemCount = BottomNavigationView(requireContext()).maxItemCount // = 5
                (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                    with (model.uiState.value.tabsChecked.count { (_, v) -> v }) { this in 1..maxItemCount}
            }

            // Also highlight button when checkbox is clicked
            checkBox.setOnTouchListener { _, motionEvent ->
                textView.dispatchTouchEvent(motionEvent)
            }

            // Do not highlight the button when the drag handle is touched
            dragHandle.setOnTouchListener { _, _ -> true }
        }

        override fun getItemCount(): Int {
            return model.uiState.value.tabsChecked.size
        }

        fun onItemMove(from: Int, to: Int) {
            val previous = model.tabsCheckedRemove(from)
            model.tabsCheckedAdd(to, previous)
            notifyItemMoved(from, to)
            notifyItemChanged(to) // necessary to avoid checkBox issues
        }
    }
}

class ArrangeTabsViewModelFactory constructor(
    private val applicationContext: Context, private val db: AppDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArrangeTabsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArrangeTabsViewModel(applicationContext, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@HiltViewModel
class ArrangeTabsViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val db: AppDatabase
): ViewModel() {

    private val _uiState = MutableStateFlow(ArrangeTabsUiState())
    val uiState: StateFlow<ArrangeTabsUiState> = _uiState

    private var oldTabsChecked: MutableList<Pair<Tab, Boolean>> = mutableListOf()

    init {
        initTabsDbEntities()
    }

    private fun initTabsDbEntities() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                userId = db.userDao().getActiveUser()!!.user_id,
                instanceUri = db.instanceDao().getActiveInstance().uri,
            )
        }
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsDbEntities = db.tabsDao().getTabsChecked(_uiState.value.userId, _uiState.value.instanceUri)
            )
        }
    }

    fun initTabsChecked(view: View) {
        if (oldTabsChecked.isEmpty()) {
            // Only load tabsChecked if the model has not been updated
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    tabsChecked = if (_uiState.value.tabsDbEntities.isEmpty()) {
                        // Load default menu
                        val list = loadDefaultMenuTabs(applicationContext, view)
                        list.zip(List(list.size){true}.toTypedArray()).toList()
                    } else {
                        // Get current menu visibility and order from settings
                        loadDbMenuTabs(applicationContext, _uiState.value.tabsDbEntities).toList()
                    }
                )
            }
        }
    }

    fun tabsCheckReplace(position: Int, pair: Pair<Tab, Boolean>) {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        oldTabsChecked[position] = pair
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
    }

    fun tabsCheckedRemove(position: Int): Pair<Tab, Boolean> {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        val removedPair = oldTabsChecked.removeAt(position)
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
        return removedPair
    }

    fun tabsCheckedAdd(position: Int, pair: Pair<Tab, Boolean>) {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        oldTabsChecked.add(position, pair)
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
    }
}

data class ArrangeTabsUiState(
    val userId: String = "",
    val instanceUri: String = "",
    val tabsDbEntities: List<TabsDatabaseEntity> = listOf(),
    val tabsChecked: List<Pair<Tab, Boolean>> = listOf()
)