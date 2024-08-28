package org.pixeldroid.app.directmessages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityFollowersBinding
import org.pixeldroid.app.databinding.NewDmDialogBinding
import org.pixeldroid.app.utils.BaseActivity


class DirectMessagesActivity : BaseActivity() {
    lateinit var binding: ActivityFollowersBinding

    private lateinit var conversationFragment: DirectMessagesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowersBinding.inflate(layoutInflater)

        conversationFragment = DirectMessagesFragment()

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.direct_messages)

        initConversationFragment(savedInstanceState)

        addFab()
    }

    private fun addFab() {
        // Create a Floating Action Button
        val fab = FloatingActionButton(this).apply {
            id = View.generateViewId()
            setImageResource(android.R.drawable.ic_dialog_email) // Example icon
            ViewCompat.setElevation(this, 8f) // Set elevation if needed
        }

        // Set LayoutParams for the FAB
        val fabParams = CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = android.view.Gravity.END or android.view.Gravity.BOTTOM
            marginEnd = 16
            bottomMargin = 16
        }

        // Add the FAB to the CoordinatorLayout
        binding.coordinatorFollowers.addView(fab, fabParams)

        fab.setOnClickListener {
            newDirectMessage()
        }
    }

    private fun newDirectMessage() {
        val newDmDialogBinding =
            NewDmDialogBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.new_dm_conversation)
            .setMessage(R.string.dm_instruction)
            .setView(newDmDialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = newDmDialogBinding.dmTarget.text.toString()
                val text = newDmDialogBinding.dmText.text.toString()
                lifecycleScope.launch {
                    try {
                        apiHolder.api?.let {
                            val pid = it.lookupUser(name, remote = name.count { it == '@' } == 2)
                                .firstOrNull {
                                    it.name == name
                                }?.id ?: return@launch errorSending()
                            it.sendDirectMessage(pid, text)
                        }
                    } catch (e: Exception) {
                        Log.e("DirectMessagesActivity", e.toString())
                        errorSending()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
//
//        conversation?.accounts?.firstOrNull()?.let {
//            val intent = Intent(itemView.context, ConversationActivity::class.java).apply {
//                putExtra(PROFILE_ID, it.id)
//                putExtra(CONVERSATION_ID, conversation?.id)
//                putExtra(USERNAME, it.getDisplayName())
//            }
//            startActivity(intent)
//        }
    }

    private fun errorSending() {
        Snackbar.make(binding.root, R.string.new_dm_error, Snackbar.LENGTH_LONG).show()
    }

    private fun initConversationFragment(savedInstanceState: Bundle?) {
        //TODO finish work here! commentFragment needs the swiperefreshlayout.. how??
        //Maybe read https://archive.ph/G9VHW#selection-1324.2-1322.3 or further research
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.conversationFragment, conversationFragment)
            }
        }
    }
}