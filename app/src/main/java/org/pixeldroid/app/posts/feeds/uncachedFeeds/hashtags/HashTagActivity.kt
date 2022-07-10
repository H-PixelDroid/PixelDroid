package org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags

import android.os.Bundle
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedPostsFragment
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.api.objects.Tag.Companion.HASHTAG_TAG


class HashTagActivity : BaseThemedWithBarActivity() {
    private var tagFragment = UncachedPostsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get hashtag tag
        val tag = intent.getSerializableExtra(HASHTAG_TAG) as String?

        startFragment(tag!!)
    }

    private fun startFragment(tag : String) {
        supportActionBar?.title = getString(R.string.hashtag_title).format(tag)

        val arguments = Bundle()
        arguments.putSerializable(HASHTAG_TAG, tag)
        tagFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.followsFragment, tagFragment).commit()

    }
}
