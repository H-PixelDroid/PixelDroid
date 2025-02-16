package org.pixeldroid.app.searchDiscover

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityTrendingBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.feeds.uncachedFeeds.accountLists.AccountViewHolder
import org.pixeldroid.app.posts.feeds.uncachedFeeds.search.HashTagViewHolder
import org.pixeldroid.app.profile.ProfilePostViewHolder
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Tag
import org.pixeldroid.app.utils.setSquareImageFromURL

class TrendingActivity : BaseActivity() {

    private lateinit var binding: ActivityTrendingBinding
    private lateinit var trendingAdapter : TrendingRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityTrendingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        val recycler = binding.list
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val type = intent.getSerializableExtra(TRENDING_TAG) as TrendingType? ?: TrendingType.POSTS

        when (type) {
            TrendingType.POSTS, TrendingType.DISCOVER -> {
                // Set posts RecyclerView as a grid with 3 columns
                recycler.layoutManager = GridLayoutManager(this, 3)
                supportActionBar?.setTitle(
                    if (type == TrendingType.POSTS) {
                        R.string.trending_posts
                    } else {
                        R.string.discover
                    }
                )
                this.trendingAdapter = DiscoverRecyclerViewAdapter()
            }
            TrendingType.HASHTAGS -> {
                supportActionBar?.setTitle(R.string.trending_hashtags)
                this.trendingAdapter = HashtagsRecyclerViewAdapter()
            }
            TrendingType.ACCOUNTS -> {
                supportActionBar?.setTitle(R.string.popular_accounts)
                this.trendingAdapter = AccountsRecyclerViewAdapter()
            }
        }
        recycler.adapter = this.trendingAdapter

        getTrending(type)
        binding.refreshLayout.setOnRefreshListener {
            getTrending(type)
        }
    }

    private fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        binding.motionLayout.apply {
            if(show){
                transitionToEnd()
                binding.errorLayout.errorText.setText(errorText)
            } else {
                transitionToStart()
            }
        }
        binding.refreshLayout.isRefreshing = false
        binding.progressBar.visibility = View.GONE
    }

    private fun getTrending(type: TrendingType) {
        lifecycleScope.launchWhenCreated {
            try {
                val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                val content: List<FeedContent> = when(type) {
                    TrendingType.POSTS ->  api.trendingPosts(Range.daily)
                    TrendingType.HASHTAGS -> api.trendingHashtags().map { it.copy(name = it.name.removePrefix("#")) }
                    TrendingType.ACCOUNTS -> api.popularAccounts()
                    TrendingType.DISCOVER -> api.discover().posts
                }
                trendingAdapter.addPosts(content)
                showError(show = false)
            } catch (exception: Exception) {
                showError()
            }
        }
    }

    /**
     * Abstract class for the different RecyclerViewAdapters used in this activity
     */
    abstract class TrendingRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        val data: ArrayList<FeedContent?> = ArrayList()

        @SuppressLint("NotifyDataSetChanged")
        fun addPosts(newPosts: List<FeedContent>){
            data.clear()
            data.addAll(newPosts)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = data.size
    }

    /**
     * [RecyclerView.Adapter] that can display a list of [Status]s' thumbnails for the discover view
     */
    class DiscoverRecyclerViewAdapter: TrendingRecyclerViewAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder =
            ProfilePostViewHolder.create(parent)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder !is ProfilePostViewHolder) return

            val post = data[position] as? Status
            if((post?.media_attachments?.size ?: 0) > 1) {
                holder.albumIcon.visibility = View.VISIBLE
            } else {
                holder.albumIcon.visibility = View.GONE
                if(post?.media_attachments?.getOrNull(0)?.type == Attachment.AttachmentType.video) {
                    holder.videoIcon.visibility = View.VISIBLE
                } else holder.videoIcon.visibility = View.GONE

            }
            setSquareImageFromURL(holder.postView.root, post?.getPostPreviewURL(), holder.postPreview, post?.media_attachments?.firstOrNull()?.blurhash)
            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postView.root.context, PostActivity::class.java)
                intent.putExtra(Status.POST_TAG, post)
                holder.postView.root.context.startActivity(intent)
            }
        }

    }

    companion object {
        const val TRENDING_TAG = "TrendingTag"

        enum class TrendingType {
            POSTS, HASHTAGS, ACCOUNTS, DISCOVER
        }

        @Suppress("EnumEntryName", "unused")
        enum class Range {
            daily, monthly, yearly
        }
    }


    /**
     * [RecyclerView.Adapter] that can display a list of [Tag]s for the trending view
     */
    class HashtagsRecyclerViewAdapter: TrendingRecyclerViewAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HashTagViewHolder =
            HashTagViewHolder.create(parent)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val tag = data[position] as Tag
            (holder as HashTagViewHolder).bind(tag)
        }
    }


    /**
     * [RecyclerView.Adapter] that can display a list of [Account]s for the popular view
     */
    class AccountsRecyclerViewAdapter: TrendingRecyclerViewAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder =
            AccountViewHolder.create(parent)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val account = data[position] as? Account
            (holder as AccountViewHolder).bind(account)
        }
    }
}