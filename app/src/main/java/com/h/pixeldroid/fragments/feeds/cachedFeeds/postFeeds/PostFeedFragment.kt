package com.h.pixeldroid.fragments.feeds.cachedFeeds.postFeeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.db.dao.feedContent.FeedContentDao
import com.h.pixeldroid.fragments.StatusViewHolder
import com.h.pixeldroid.fragments.feeds.cachedFeeds.FeedViewModel
import com.h.pixeldroid.fragments.feeds.cachedFeeds.CachedFeedFragment
import com.h.pixeldroid.fragments.feeds.cachedFeeds.ViewModelFactory
import com.h.pixeldroid.objects.FeedContentDatabase
import com.h.pixeldroid.objects.Status


/**
 * Fragment for the home feed or public feed tabs.
 *
 * Takes a "home" boolean in its arguments [Bundle] to determine which
 */
@ExperimentalPagingApi
class PostFeedFragment<T: FeedContentDatabase>: CachedFeedFragment<T>() {

    private lateinit var mediator: RemoteMediator<Int, T>
    private lateinit var dao: FeedContentDao<T>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = PostsAdapter()

        @Suppress("UNCHECKED_CAST")
        if (requireArguments().get("home") as Boolean){
            mediator = HomeFeedRemoteMediator(apiHolder, db) as RemoteMediator<Int, T>
            dao = db.homePostDao() as FeedContentDao<T>
        }
        else {
            mediator = PublicFeedRemoteMediator(apiHolder, db) as RemoteMediator<Int, T>
            dao = db.publicPostDao() as FeedContentDao<T>
        }
    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(db, dao, mediator))
            .get(FeedViewModel::class.java) as FeedViewModel<T>

        launch()
        initSearch()

        return view
    }

    inner class PostsAdapter : PagingDataAdapter<T, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
                oldItem.id == newItem.id
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return StatusViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.post_fragment
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position) as Status
            uiModel.let {
                val instanceUri = db.userDao().getActiveUser()!!.instance_uri
                val accessToken = db.userDao().getActiveUser()!!.accessToken
                (holder as StatusViewHolder).bind(it, instanceUri, apiHolder.setDomain(instanceUri), "Bearer $accessToken")
            }
        }
    }
}