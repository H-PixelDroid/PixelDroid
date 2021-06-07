package org.pixeldroid.app.utils.api.objects

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags.HashTagActivity
import java.io.Serializable

data class Tag(
    //Base attributes
    val name: String,
    val url: String,
    //Optional attributes
    val history: List<History>? = emptyList()) : Serializable, FeedContent {
    //needed to be a FeedContent, this inheritance is a bit fickle. Do not use.
    override val id: String
        get() = "tag"

    companion object {
        const val HASHTAG_TAG = "HashtagTag"

        fun openTag(context: Context, tag: String) {
            val intent = Intent(context, HashTagActivity::class.java)
            intent.putExtra(HASHTAG_TAG, tag)
            ContextCompat.startActivity(context, intent, null)
        }
    }
}

