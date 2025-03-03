package org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityFollowersBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedPostsFragment
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.objects.Tag.Companion.HASHTAG_TAG


class HashTagActivity : BaseActivity() {
    private lateinit var binding: ActivityFollowersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFollowersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get hashtag tag
        val tag = intent.getSerializableExtra(HASHTAG_TAG) as String?

        startFragment(tag!!)
    }

    private fun startFragment(tag : String) {
        supportActionBar?.title = getString(R.string.hashtag_title).format(tag)

        val arguments = Bundle()
        arguments.putSerializable(HASHTAG_TAG, tag)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<UncachedPostsFragment>(R.id.conversationFragment, args = arguments)
        }
    }
}
