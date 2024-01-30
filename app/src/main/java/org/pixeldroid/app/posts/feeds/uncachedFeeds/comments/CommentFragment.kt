package org.pixeldroid.app.posts.feeds.uncachedFeeds.comments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.CommentBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedFeedFragment
import org.pixeldroid.app.posts.feeds.uncachedFeeds.ViewModelFactory
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.setProfileImageFromURL


/**
 * Fragment to show a list of [Status]s, in form of comments
 */
class CommentFragment(val swipeRefreshLayout: SwipeRefreshLayout): UncachedFeedFragment<Status>() {

    private lateinit var id: String
    private lateinit var domain: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments?.getSerializable(COMMENT_STATUS_ID) as String
        domain = arguments?.getSerializable(COMMENT_DOMAIN) as String

        adapter = CommentAdapter()
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {


        val view = super.onCreateView(inflater, container, savedInstanceState, swipeRefreshLayout)

        // Get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(
            requireActivity(), ViewModelFactory(
                CommentContentRepository(
                    apiHolder.setToCurrentUser(),
                    id
                )
            )
        )["commentFragment", FeedViewModel::class.java] as FeedViewModel<Status>

        launch()
        initSearch()

        binding?.swipeRefreshLayout?.isEnabled = false
        return view
    }
    companion object {
        const val COMMENT_STATUS_ID = "PostActivityCommentsId"
        const val COMMENT_DOMAIN = "PostActivityCommentsDomain"
    }


    private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
        override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
            oldItem.content == newItem.content
    }

    inner class CommentAdapter : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
        UIMODEL_COMPARATOR
    ) {
        fun create(parent: ViewGroup): CommentViewHolder {
            val itemBinding = CommentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return CommentViewHolder(itemBinding)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return create(parent)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val post = getItem(position)

            post?.let {
                (holder as CommentViewHolder).bind(it)
            }
        }
    }


    inner class CommentViewHolder(val binding: CommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Status) {

            setProfileImageFromURL(
                binding.profilePic,
                comment.account!!.anyAvatar(),
                binding.profilePic
            )
            binding.user.text = comment.account.username
            binding.commentText.text = parseHTMLText(
                comment.content!!,
                comment.mentions.orEmpty(),
                apiHolder,
                itemView.context,
                lifecycleScope
            )

            binding.postDomain.text =
                comment.getStatusDomain(domain, binding.postDomain.context)

            if (comment.replies_count == 0 || comment.replies_count == null) {
                binding.replies.visibility = View.GONE
            } else {
                binding.replies.visibility = View.VISIBLE
                binding.replies.text = itemView.context.resources.getQuantityString(
                    R.plurals.replies_count,
                    comment.replies_count,
                    comment.replies_count
                )
            }

            binding.comment.setOnClickListener { openComment(comment) }
            binding.profilePic.setOnClickListener { comment.account.openProfile(itemView.context) }
            binding.user.setOnClickListener { comment.account.openProfile(itemView.context) }
        }

        private fun openComment(comment: Status) {
            val intent = Intent(itemView.context, PostActivity::class.java).apply {
                putExtra(Status.POST_TAG, comment)
            }
            itemView.context.startActivity(intent)
        }
    }
}