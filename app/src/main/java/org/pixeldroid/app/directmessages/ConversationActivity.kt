package org.pixeldroid.app.directmessages

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityConversationBinding
import org.pixeldroid.app.directmessages.ConversationFragment.Companion.CONVERSATION_ID
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI

class ConversationActivity : BaseActivity() {
    lateinit var binding: ActivityConversationBinding

    private lateinit var conversationFragment: ConversationFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)

        conversationFragment = ConversationFragment()

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.title = getString(R.string.post_title)
        val conversationId = intent?.getSerializableExtra(CONVERSATION_ID) as String

        //TODO conversationId is not pid and sending a DM to it will not work :see-no-evil:
        activateCommenter(conversationId)

        initConversationFragment(conversationId, savedInstanceState)
    }

    private fun activateCommenter(pid: String) {
        //Activate commenter
        binding.submitComment.setOnClickListener {
            val textIn = binding.editComment.text
            //Open text input
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.empty_comment),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //Post the comment
                lifecycleScope.launchWhenCreated {
                    apiHolder.api?.let { it1 -> sendMessage(it1, pid) }
                }
            }
        }
    }

    private fun initConversationFragment(conversationId: String, savedInstanceState: Bundle?) {

        val arguments = Bundle()
        arguments.putSerializable(CONVERSATION_ID, conversationId)
        conversationFragment.arguments = arguments

        //TODO finish work here! commentFragment needs the swiperefreshlayout.. how??
        //Maybe read https://archive.ph/G9VHW#selection-1324.2-1322.3 or further research
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.conversationFragment, conversationFragment)
            }
        }
    }

    private suspend fun sendMessage(
        api: PixelfedAPI,
        pid: String,
    ) {
        val textIn = binding.editComment.text
        val nonNullText = textIn.toString()
        try {
            api.sendDirectMessage(pid, nonNullText)
            binding.commentIn.visibility = View.GONE

            //Reload to add the comment to the comment section
            conversationFragment.adapter.refresh()

            Toast.makeText(
                binding.root.context,
                binding.root.context.getString(R.string.comment_posted).format(textIn),
                Toast.LENGTH_SHORT
            ).show()
        } catch (exception: Exception) {
            Log.e("DM SEND ERROR", exception.toString())
            Toast.makeText(
                binding.root.context, binding.root.context.getString(R.string.comment_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
