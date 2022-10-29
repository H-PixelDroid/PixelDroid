package org.pixeldroid.app.searchDiscover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Tag
import org.pixeldroid.app.utils.setSquareImageFromURL
import retrofit2.HttpException
import java.io.IOException

class TrendingActivity : BaseThemedWithBarActivity() {

    private lateinit var api: PixelfedAPI
    private lateinit var binding: ActivityTrendingBinding
    private lateinit var recycler : RecyclerView
    private lateinit var discoverAdapter : DiscoverRecyclerViewAdapter
    private lateinit var hashtagsAdapter : HashtagsRecyclerViewAdapter
    private lateinit var accountsAdapter : AccountsRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrendingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = apiHolder.api ?: apiHolder.setToCurrentUser()
        recycler = binding.list
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val type = intent.getSerializableExtra(TRENDING_TAG) as TrendingType? ?: TrendingType.POSTS

        if(type == TrendingType.POSTS || type == TrendingType.DISCOVER) {
            // Set posts RecyclerView as a grid with 3 columns
            recycler.layoutManager = GridLayoutManager(this, 3)
            discoverAdapter = DiscoverRecyclerViewAdapter()
            recycler.adapter = discoverAdapter
            if(type == TrendingType.POSTS) {
                supportActionBar?.setTitle(R.string.trending_posts)
            } else {
                supportActionBar?.setTitle(R.string.discover)
            }
        }
        if(type == TrendingType.HASHTAGS) {
            supportActionBar?.setTitle(R.string.trending_hashtags)
            hashtagsAdapter = HashtagsRecyclerViewAdapter()
            recycler.adapter = hashtagsAdapter
        }
        if(type == TrendingType.ACCOUNTS) {
            supportActionBar?.setTitle(R.string.popular_accounts)
            accountsAdapter = AccountsRecyclerViewAdapter()
            recycler.adapter = accountsAdapter
        }

        getTrending(type)
        binding.refreshLayout.setOnRefreshListener {
            getTrending(type)
        }
    }

    private fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        binding.motionLayout.apply {
            if(show){
                transitionToEnd()
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
                when(type) {
                    TrendingType.POSTS -> {
                        val trendingPosts = api.trendingPosts("daily")
                        discoverAdapter.addPosts(trendingPosts)
                    }
                    TrendingType.HASHTAGS -> {
                        val trendingTags = api.trendingHashtags()
                            .map { it.copy(name = it.name.removePrefix("#")) }
                        hashtagsAdapter.addHashtags(trendingTags)
                    }
                    TrendingType.ACCOUNTS -> {
                        val trendingAccounts = api.popularAccounts()
                        accountsAdapter.addAccounts(trendingAccounts)
                    }
                    TrendingType.DISCOVER -> {
                        val posts = api.discover().posts
                        discoverAdapter.addPosts(posts)
                    }
                }
                showError(show = false)
            } catch (exception: IOException) {
                showError()
            } catch (exception: HttpException) {
                showError()
            }
        }
    }

    /**
     * [RecyclerView.Adapter] that can display a list of [Status]s' thumbnails for the discover view
     */
    class DiscoverRecyclerViewAdapter: RecyclerView.Adapter<ProfilePostViewHolder>() {
        private val posts: ArrayList<Status?> = ArrayList()

        fun addPosts(newPosts : List<Status>) {
            posts.clear()
            posts.addAll(newPosts)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_profile_posts, parent, false)
            return ProfilePostViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
            val post = posts[position]
            if((post?.media_attachments?.size ?: 0) > 1) {
                holder.albumIcon.visibility = View.VISIBLE
            } else {
                holder.albumIcon.visibility = View.GONE
                if(post?.media_attachments?.getOrNull(0)?.type == Attachment.AttachmentType.video) {
                    holder.videoIcon.visibility = View.VISIBLE
                } else holder.videoIcon.visibility = View.GONE

            }
            setSquareImageFromURL(holder.postView, post?.getPostPreviewURL(), holder.postPreview, post?.media_attachments?.firstOrNull()?.blurhash)
            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postView.context, PostActivity::class.java)
                intent.putExtra(Status.POST_TAG, post)
                holder.postView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = posts.size
    }

    companion object {
        const val TRENDING_TAG = "TrendingTag"

        enum class TrendingType {
            POSTS, HASHTAGS, ACCOUNTS, DISCOVER
        }
    }


    /**
     * [RecyclerView.Adapter] that can display a list of [Tag]s for the trending view
     */
    class HashtagsRecyclerViewAdapter: RecyclerView.Adapter<HashTagViewHolder>() {
        private val tags: ArrayList<Tag?> = ArrayList()

        fun addHashtags(newTags : List<Tag>) {
            tags.clear()
            tags.addAll(newTags)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HashTagViewHolder {
            return HashTagViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: HashTagViewHolder, position: Int) {
            val tag = tags[position]
            holder.bind(tag)
        }

        override fun getItemCount(): Int = tags.size
    }


    /**
     * [RecyclerView.Adapter] that can display a list of [Account]s for the popular view
     */
    class AccountsRecyclerViewAdapter: RecyclerView.Adapter<AccountViewHolder>() {
        private val accounts: ArrayList<Account?> = ArrayList()

        fun addAccounts(newAccounts : List<Account>) {
            accounts.clear()
            accounts.addAll(newAccounts)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
            return AccountViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            val account = accounts[position]
            holder.bind(account)
        }

        override fun getItemCount(): Int = accounts.size
    }
}