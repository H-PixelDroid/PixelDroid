package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.LayoutTabsArrangeBinding
import org.pixeldroid.app.utils.Tab
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity
import javax.inject.Inject

@AndroidEntryPoint
class ArrangeTabsFragment: DialogFragment() {

    private lateinit var binding: LayoutTabsArrangeBinding

    @Inject
    lateinit var db: AppDatabase

    private val model: ArrangeTabsViewModel by viewModels()

    var showTutorial = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = LayoutTabsArrangeBinding.inflate(layoutInflater)

        val itemCount = model.initTabsChecked()
        model.initTabsButtons(itemCount, requireContext())

        val listFeed: RecyclerView = binding.tabs
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
            setView(binding.root)
            setNegativeButton(android.R.string.cancel) { _, _ -> }
            setPositiveButton(android.R.string.ok) { _, _ ->
                // Save values into preferences
                val tabsChecked = listAdapter.model.uiState.value.tabsChecked.toList()
                val tabsDbEntity = tabsChecked.mapIndexed { index, (tab, checked) -> with (db.userDao().getActiveUser()!!) {
                    TabsDatabaseEntity(index, user_id, instance_uri, tab.name, checked, tab.filter)
                } }
                lifecycleScope.launch {
                    db.tabsDao().clearAndRefill(tabsDbEntity, model.uiState.value.userId, model.uiState.value.instanceUri)
                }
            }
        }.create()
        if (showTutorial) showTutorial(dialog)
        return dialog
    }

    private fun showTutorial(dialog: Dialog){
        lifecycleScope.launch {
            var handle = binding.tabs.findViewHolderForLayoutPosition(0)?.itemView?.findViewById<ImageView>(R.id.dragHandle)
            while (handle == null) {
                handle = binding.tabs.findViewHolderForLayoutPosition(0)?.itemView?.findViewById(R.id.dragHandle)
                delay(100)
            }
            TapTargetView.showFor(
                dialog,
                TapTarget.forView(handle, getString(R.string.drag_customtabs_tutorial))
                    .transparentTarget(true)
                    .targetRadius(60),  // Specify the target radius (in dp)
                object : TapTargetView.Listener() {
                    // The listener can listen for regular clicks, long clicks or cancels
                    override fun onTargetClick(view: TapTargetView?) {
                        super.onTargetClick(view) // This call is optional
                        // Perform action for the current target
                        val checkBox = binding.tabs.findViewHolderForLayoutPosition(0)?.itemView?.findViewById<View>(R.id.checkBox)
                        TapTargetView.showFor(
                            dialog,
                            TapTarget.forView(checkBox,
                                getString(R.string.de_activate_tabs_tutorial))
                                .transparentTarget(true)
                                .targetRadius(60),  // Specify the target radius (in dp)
                            object : TapTargetView.Listener() {
                                // The listener can listen for regular clicks, long clicks or cancels
                                override fun onTargetClick(view: TapTargetView?) {
                                    super.onTargetClick(view) // This call is optional
                                    // Perform action for the current target
                                    val index = (Tab.defaultTabs + Tab.otherTabs).size - 1
                                    binding.tabs.scrollToPosition(index)
                                    lifecycleScope.launch {
                                        var hashtag =
                                            binding.tabs.findViewHolderForLayoutPosition(index)?.itemView?.findViewById<View>(
                                                R.id.textView
                                            )
                                        while (hashtag == null) {
                                            hashtag =
                                                binding.tabs.findViewHolderForLayoutPosition(index)?.itemView?.findViewById(
                                                    R.id.textView
                                                )
                                            delay(100)
                                        }
                                        TapTargetView.showFor(
                                            dialog,
                                            TapTarget.forView(
                                                hashtag,
                                                getString(R.string.custom_feed_tutorial_title),
                                                getString(R.string.custom_feed_tutorial_explanation)
                                            )
                                                .transparentTarget(true)
                                                .targetRadius(60),  // Specify the target radius (in dp)
                                            object : TapTargetView.Listener() {
                                                // The listener can listen for regular clicks, long clicks or cancels
                                                override fun onTargetClick(view: TapTargetView?) {
                                                    super.onTargetClick(view) // This call is optional
                                                    // Perform action for the current target
                                                }
                                            })
                                    }
                                }
                            })
                    }
                })
        }
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

            val tabText = model.getTabsButtons(position)

            // Set content of each entry
            if (tabText != null) {
                textView.text = tabText
            } else {
                model.updateTabsButtons(position, model.uiState.value.tabsChecked[position].first.toLanguageString(requireContext(), db, position, true))
                textView.text = model.getTabsButtons(position)
            }
            checkBox.isChecked = model.uiState.value.tabsChecked[position].second

            fun callbackOnClickListener(tabNew: Tab, isCheckedNew: Boolean, hashtag: String? = null) {
                tabNew.filter = hashtag?.split("#")?.filter { it.isNotBlank() }?.get(0)?.trim()
                model.tabsCheckReplace(position, Pair(tabNew, isCheckedNew))
                checkBox.isChecked = model.uiState.value.tabsChecked[position].second

                val hashtagWithHashtag = tabNew.filter?.let {
                    StringBuilder("#").append(it).toString()
                }

                // Disable OK button when no tab is selected or when strictly more than 5 tabs are selected
                val maxItemCount = BottomNavigationView(requireContext()).maxItemCount // = 5
                (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                    with (model.uiState.value.tabsChecked.count { (_, v) -> v }) { this in 1..maxItemCount}
                model.updateTabsButtons(position, hashtagWithHashtag ?: model.uiState.value.tabsChecked[position].first.toLanguageString(requireContext(), db, position, false))
                textView.text = model.getTabsButtons(position)
            }

            // Also interact with checkbox when button is clicked
            textView.setOnClickListener {
                val isCheckedNew = !model.uiState.value.tabsChecked[position].second
                val tabNew = model.uiState.value.tabsChecked[position].first

                if (tabNew == Tab.HASHTAG_FEED && isCheckedNew) {
                    // Ask which hashtag should filter
                    val textField = EditText(requireContext())

                    textField.hint = "hashtag"

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.feed_hashtag))
                        .setMessage(getString(R.string.feed_hashtag_description))
                        .setView(textField)
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val hashtag = textField.text.toString()
                            callbackOnClickListener(tabNew, isCheckedNew, hashtag)
                        }
                        .show()
                } else {
                    callbackOnClickListener(tabNew, isCheckedNew)
                }
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
            model.swapTabsButtons(from, to)
            notifyItemMoved(from, to)
            notifyItemChanged(to) // necessary to avoid checkBox issues
        }
    }
}
