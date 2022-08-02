package org.pixeldroid.app.posts

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostBinding
import org.pixeldroid.app.databinding.CommentBinding
import org.pixeldroid.app.posts.feeds.initAdapter
import org.pixeldroid.app.posts.feeds.launch
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.uncachedFeeds.comments.CommentContentRepository
import org.pixeldroid.app.profile.ProfileViewModelFactory
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_COMMENT_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import org.pixeldroid.app.utils.displayDimensionsInPx
import org.pixeldroid.app.utils.setProfileImageFromURL
import retrofit2.HttpException
import java.io.IOException

class PostActivity : BaseThemedWithBarActivity() {
    lateinit var domain : String

    private lateinit var binding: ActivityPostBinding
    private lateinit var profileAdapter: PagingDataAdapter<Status, RecyclerView.ViewHolder>
    private lateinit var commentViewModel: FeedViewModel<Status>
    private var job: Job? = null


    private lateinit var status: Status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        status = intent.getSerializableExtra(POST_TAG) as Status
        val viewComments: Boolean = intent.getBooleanExtra(VIEW_COMMENTS_TAG, false)
        val postComment: Boolean = intent.getBooleanExtra(POST_COMMENT_TAG, false)

        val user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()

        supportActionBar?.title = getString(R.string.post_title).format(status.account?.getDisplayName())

        val holder = StatusViewHolder(binding.postFragmentSingle)

        holder.bind(status, apiHolder, db, lifecycleScope, displayDimensionsInPx(), isActivity = true)

        activateCommenter()
        retrieveComments()

        if(viewComments || postComment){
            //Scroll already down as much as possible (since comments are not loaded yet)
            binding.scrollview.requestChildFocus(binding.editComment, binding.editComment)

            //Open keyboard if we want to post a comment
            if(postComment && binding.editComment.requestFocus()) {
                window.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)
                binding.editComment.requestFocus()
            }
        }
    }

    private fun activateCommenter() {
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
                    apiHolder.api?.let { it1 -> postComment(it1) }
                }
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun retrieveComments() {
            // get the view model
            @Suppress("UNCHECKED_CAST")
            commentViewModel = ViewModelProvider(this@PostActivity, ProfileViewModelFactory(
                CommentContentRepository(
                    apiHolder.setToCurrentUser(),
                    status.id
                )
            )
            )[FeedViewModel::class.java] as FeedViewModel<Status>

            profileAdapter = CommentAdapter()
            initAdapter(binding.postCommentsProgressBar, binding.postRefreshLayout,
                binding.commentRecyclerView, binding.motionLayout, binding.errorLayout,
                profileAdapter)

            job = launch(job, lifecycleScope, commentViewModel, profileAdapter)
    }

    private suspend fun postComment(
        api: PixelfedAPI,
    ) {
        val textIn = binding.editComment.text
        val nonNullText = textIn.toString()
        status.id.let {
            try {
                val response = api.postStatus(nonNullText, it)
                binding.commentIn.visibility = View.GONE

                //Add the comment to the comment section
                profileAdapter.refresh()

                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.comment_posted).format(textIn),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: IOException) {
                Log.e("COMMENT ERROR", exception.toString())
                Toast.makeText(
                    binding.root.context, binding.root.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: HttpException) {
                Toast.makeText(
                    binding.root.context, binding.root.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ERROR_CODE", exception.code().toString())
            }
        }
    }


    inner class CommentViewHolder(val binding: CommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Status) {

            setProfileImageFromURL(binding.profilePic,
                comment.account!!.anyAvatar(),
                binding.profilePic
            )
            binding.user.text = comment.account.username
            binding.commentText.text = parseHTMLText(
                comment.content!!,
                comment.mentions.orEmpty(),
                apiHolder,
                this@PostActivity,
                lifecycleScope
            )

            if(comment.replies_count == 0 || comment.replies_count == null){
                binding.replies.visibility = View.GONE
            } else {
                binding.replies.visibility = View.VISIBLE
                binding.replies.text = resources.getQuantityString(
                    R.plurals.replies_count,
                    comment.replies_count,
                    comment.replies_count
                )
            }

            binding.comment.setOnClickListener{ openComment(comment) }
            binding.profilePic.setOnClickListener{ comment.account.openProfile(itemView.context) }
            binding.user.setOnClickListener { comment.account.openProfile(itemView.context) }
        }

        private fun openComment(comment: Status) {
            val intent = Intent(itemView.context, PostActivity::class.java).apply {
                putExtra(POST_TAG, comment)
            }
            itemView.context.startActivity(intent)
        }
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


    private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
        override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
            oldItem.content == newItem.content
    }
}
