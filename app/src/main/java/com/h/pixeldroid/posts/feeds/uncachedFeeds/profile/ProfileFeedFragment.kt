package com.h.pixeldroid.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.FragmentProfilePostsBinding
import com.h.pixeldroid.posts.PostActivity
import com.h.pixeldroid.posts.feeds.uncachedFeeds.FeedViewModel
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedFeedFragment
import com.h.pixeldroid.posts.feeds.uncachedFeeds.ViewModelFactory
import com.h.pixeldroid.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.utils.api.objects.Status

/**
 * Fragment to show all the posts of a user.
 */
class ProfileFeedFragment : UncachedFeedFragment<Status>() {

    private lateinit var accountId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ProfileAdapter()

        accountId = arguments?.getSerializable(ACCOUNT_ID_TAG) as String

    }

    @ExperimentalPagingApi
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(
                ProfileContentRepository(
                        apiHolder.setDomainToCurrentUser(db),
                        db.userDao().getActiveUser()!!.accessToken,
                        accountId
                )
            )
        ).get(FeedViewModel::class.java) as FeedViewModel<Status>

        launch()
        initSearch()

        return view
    }
}


class PostViewHolder(binding: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(binding.root) {
    private val postPreview: ImageView = binding.postPreview
    private val albumIcon: ImageView = binding.albumIcon

    fun bind(post: Status) {

        if(post.sensitive!!) {
            ImageConverter.setSquareImageFromDrawable(
                    itemView,
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_sensitive),
                    postPreview
            )
        } else {
            ImageConverter.setSquareImageFromURL(itemView, post.getPostPreviewURL(), postPreview)
        }

        if(post.media_attachments?.size ?: 0 > 1) {
            albumIcon.visibility = View.VISIBLE
        } else {
            albumIcon.visibility = View.GONE
        }

        postPreview.setOnClickListener {
            val intent = Intent(postPreview.context, PostActivity::class.java)
            intent.putExtra(Status.POST_TAG, post)
            postPreview.context.startActivity(intent)
        }
    }

    companion object {
        fun create(parent: ViewGroup): PostViewHolder {
            val itemBinding = FragmentProfilePostsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
            )
            return PostViewHolder(itemBinding)
        }
    }
}


class ProfileAdapter : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
    UIMODEL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PostViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = getItem(position)

        post?.let {
            (holder as PostViewHolder).bind(it)
        }
    }

    companion object {
        private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
                    oldItem.content == newItem.content
        }
    }

}