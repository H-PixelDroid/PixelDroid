package org.pixeldroid.app.posts

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.comments.CommentFragment
import org.pixeldroid.app.posts.feeds.uncachedFeeds.comments.CommentFragment.Companion.COMMENT_DOMAIN
import org.pixeldroid.app.posts.feeds.uncachedFeeds.comments.CommentFragment.Companion.COMMENT_STATUS_ID
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_COMMENT_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import org.pixeldroid.app.utils.displayDimensionsInPx

class PostActivity : BaseActivity() {
    private lateinit var binding: ActivityPostBinding

    private var commentFragment = CommentFragment()

    private lateinit var status: Status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        status = intent.getSerializableExtra(POST_TAG) as Status
        val viewComments: Boolean = intent.getBooleanExtra(VIEW_COMMENTS_TAG, false)
        val postComment: Boolean = intent.getBooleanExtra(POST_COMMENT_TAG, false)

        val user = db.userDao().getActiveUser()

        supportActionBar?.title = getString(R.string.post_title).format(status.account?.getDisplayName())

        val holder = StatusViewHolder(binding.postFragmentSingle)

        holder.bind(status, apiHolder, db, lifecycleScope, displayDimensionsInPx(), isActivity = true)

        activateCommenter()
        initCommentsFragment(domain = user?.instance_uri.orEmpty())

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

    private fun initCommentsFragment(domain: String) {

        val arguments = Bundle()
        arguments.putSerializable(COMMENT_STATUS_ID, status.id)
        arguments.putSerializable(COMMENT_DOMAIN, domain)
        commentFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.commentFragment, commentFragment).commit()
    }

    private suspend fun postComment(
        api: PixelfedAPI,
    ) {
        val textIn = binding.editComment.text
        val nonNullText = textIn.toString()
        status.id.let {
            try {
                api.postStatus(nonNullText, it)
                binding.commentIn.visibility = View.GONE

                //Reload to add the comment to the comment section
                commentFragment.adapter.refresh()

                Toast.makeText(
                    binding.root.context,
                    binding.root.context.getString(R.string.comment_posted).format(textIn),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: Exception) {
                Log.e("COMMENT ERROR", exception.toString())
                Toast.makeText(
                    binding.root.context, binding.root.context.getString(R.string.comment_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
