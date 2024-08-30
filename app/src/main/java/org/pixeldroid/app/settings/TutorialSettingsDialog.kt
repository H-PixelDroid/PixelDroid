package org.pixeldroid.app.settings;

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.pixeldroid.app.R
import org.pixeldroid.app.main.MainActivity
import org.pixeldroid.app.utils.Tab

class TutorialSettingsDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arrayOf(
            Pair(R.string.feeds_tutorial, R.drawable.ic_home_white_24dp),
            Pair(R.string.create_tutorial, R.drawable.photo_camera),
            Pair(R.string.dm_tutorial, R.drawable.message),
            Pair(R.string.custom_tabs_tutorial, R.drawable.outline_bottom_navigation)
        )

        val adapter = object : ArrayAdapter<Pair<Int, Int>>(requireContext(), android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view: TextView = if (convertView == null) {
                    LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
                } else {
                    convertView as TextView
                }

                val item = getItem(position)

                if (item != null) {
                    view.setText(item.first)
                    view.setTypeface(null, Typeface.NORMAL) // Set the typeface to normal
                    view.setCompoundDrawablesWithIntrinsicBounds(item.second, 0, 0, 0)
                    view.compoundDrawablePadding = 16 // Add padding between text and drawable
                }

                view.setPadding(0, 32, 0, 32)
                return view
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.tutorial_choice))
                .setAdapter(adapter) { _, which ->
                    if(which == 3){
                        customTabsTutorial()
                        return@setAdapter
                    }
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra(START_TUTORIAL, which)
                    startActivity(intent)
                }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
        }
            .create()
    }

    private fun customTabsTutorial() {
        (requireActivity() as SettingsActivity).customTabsTutorial()
    }

    companion object {
        const val START_TUTORIAL = "tutorial_start_intent"
    }
}