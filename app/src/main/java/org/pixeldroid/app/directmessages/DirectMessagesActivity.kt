package org.pixeldroid.app.directmessages

import android.os.Bundle
import androidx.fragment.app.commit
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityConversationBinding
import org.pixeldroid.app.databinding.ActivityFollowersBinding
import org.pixeldroid.app.profile.FollowsActivity
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
