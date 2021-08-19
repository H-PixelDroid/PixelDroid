package org.pixeldroid.app.posts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostBinding
import org.pixeldroid.app.databinding.CommentBinding
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Mention
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_COMMENT_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.POST_TAG
import org.pixeldroid.app.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import org.pixeldroid.app.utils.displayDimensionsInPx
import retrofit2.HttpException
import java.io.IOException

class PostActivity : BaseActivity() {
    lateinit var domain : String

    private lateinit var binding: ActivityPostBinding

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

        if(viewComments || postComment){
            //Scroll already down as much as possible (since comments are not loaded yet)
            binding.scrollview.requestChildFocus(binding.editComment, binding.editComment)

            //Open keyboard if we want to post a comment
            if(postComment && binding.editComment.requestFocus()) {
                window.setSoftInputMode(SOFT_INPUT_STATE_VISIBLE)
                binding.editComment.requestFocus()
            }

            // also retrieve comments if we're not posting the comment
            if(!postComment) retrieveComments(apiHolder.api!!)
        }
        binding.postFragmentSingle.viewComments.setOnClickListener {
            retrieveComments(apiHolder.api!!)
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

    private fun addComment(context: Context, commentContainer: LinearLayout,
                           commentUsername: String, commentContent: String, mentions: List<Mention>) {


        val itemBinding = CommentBinding.inflate(
            LayoutInflater.from(context), commentContainer, true
        )

        itemBinding.user.text = commentUsername
        itemBinding.commentText.text = parseHTMLText(
                commentContent,
                mentions,
                apiHolder,
                context,
                lifecycleScope
        )
    }

    private fun retrieveComments(api: PixelfedAPI) {
        lifecycleScope.launchWhenCreated {
            status.id.let {
                try {
                    val statuses = api.statusComments(it).descendants

                    binding.commentContainer.removeAllViews()

                    //Create the new views for each comment
                    for (status in statuses) {
                        addComment(
                            binding.root.context,
                            binding.commentContainer,
                            status.account!!.username!!,
                            status.content!!,
                            status.mentions.orEmpty()
                        )
                    }
                    binding.commentContainer.visibility = View.VISIBLE

                    //Focus the comments
                    binding.scrollview.requestChildFocus(binding.commentContainer, binding.commentContainer)
                } catch (exception: IOException) {
                    Log.e("COMMENT FETCH ERROR", exception.toString())
                } catch (exception: HttpException) {
                    Log.e("COMMENT ERROR", "${exception.code()} with body ${exception.response()?.errorBody()}")
                }
            }
        }
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
                addComment(
                    binding.root.context, binding.commentContainer, response.account!!.username!!,
                    response.content!!, response.mentions.orEmpty()
                )

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

}
