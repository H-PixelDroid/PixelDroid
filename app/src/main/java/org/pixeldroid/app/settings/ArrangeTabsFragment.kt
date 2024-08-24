package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity
import javax.inject.Inject

@AndroidEntryPoint
class ArrangeTabsFragment: DialogFragment() {

    @Inject
    lateinit var db: AppDatabase

    private val model: ArrangeTabsViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater: LayoutInflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.layout_tabs_arrange, null)

        model.initTabsChecked()

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
                val tabsDbEntity = tabsChecked.mapIndexed { index, (tab, checked) -> with (db.userDao().getActiveUser()!!) {
                    TabsDatabaseEntity(index, user_id, instance_uri, tab.name, checked)
                } }
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