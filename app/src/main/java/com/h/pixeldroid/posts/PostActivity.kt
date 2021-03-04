package com.h.pixeldroid.posts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityPostBinding
import com.h.pixeldroid.databinding.CommentBinding
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.api.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Mention
import com.h.pixeldroid.utils.api.objects.Status.Companion.VIEW_COMMENTS_TAG
import com.h.pixeldroid.utils.displayDimensionsInPx
import retrofit2.HttpException
import java.io.IOException

class PostActivity : BaseActivity() {
    lateinit var domain : String
    private lateinit var accessToken : String

    private lateinit var binding: ActivityPostBinding

    private lateinit var status: Status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        status = intent.getSerializableExtra(POST_TAG) as Status
        val viewComments = intent.getSerializableExtra(VIEW_COMMENTS_TAG) as Boolean

        val user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        accessToken = user?.accessToken.orEmpty()


        supportActionBar?.title = getString(R.string.post_title).format(status.account?.getDisplayName())

        val holder = StatusViewHolder(binding.postFragmentSingle)

        holder.bind(status, apiHolder.api!!, db, lifecycleScope, displayDimensionsInPx(), isActivity = true)

        val credential: String = "Bearer $accessToken"
        activateCommenter(credential)

        if(viewComments){
            retrieveComments(apiHolder.api!!, credential)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun activateCommenter(credential: String) {
        //Toggle comment button
        toggleCommentInput()

        //Activate commenterpostPicture
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
                    apiHolder.api?.let { it1 -> postComment(it1, credential) }
                }
            }
        }
    }

    private fun toggleCommentInput() {
        //Toggle comment button
        binding.postFragmentSingle.commenter.setOnClickListener {
            when(binding.commentIn.visibility) {
                View.VISIBLE -> {
                    binding.commentIn.visibility = View.GONE
                    ImageConverter.setImageFromDrawable(
                        binding.root,
                        binding.postFragmentSingle.commenter,
                        R.drawable.ic_comment_empty
                    )
                }
                View.GONE -> {
                    binding.commentIn.visibility = View.VISIBLE
                    ImageConverter.setImageFromDrawable(
                        binding.root,
                        binding.postFragmentSingle.commenter,
                        R.drawable.ic_comment_blue
                    )
                }
            }
        }
    }

    fun addComment(context: Context, commentContainer: LinearLayout,
                   commentUsername: String, commentContent: String, mentions: List<Mention>,
                   credential: String) {


        val itemBinding = CommentBinding.inflate(
            LayoutInflater.from(context), commentContainer, true
        )

        itemBinding.user.text = commentUsername
        itemBinding.commentText.text = parseHTMLText(
            commentContent,
            mentions,
            apiHolder.api!!,
            context,
            credential,
            lifecycleScope
        )
    }

    private fun retrieveComments(api: PixelfedAPI, credential: String) {
        lifecycleScope.launchWhenCreated {
            status.id.let {
                try {
                    val statuses = api.statusComments(it, credential).descendants

                    binding.commentContainer.removeAllViews()

                    //Create the new views for each comment
                    for (status in statuses) {
                        addComment(binding.root.context, binding.commentContainer, status.account!!.username!!,
                            status.content!!, status.mentions.orEmpty(), credential
                        )
                    }
                    binding.commentContainer.visibility = View.VISIBLE

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
        credential: String,
    ) {
        val textIn = binding.editComment.text
        val nonNullText = textIn.toString()
        status.id.let {
            try {
                val response = api.postStatus(credential, nonNullText, it)
                binding.commentIn.visibility = View.GONE

                //Add the comment to the comment section
                addComment(
                    binding.root.context, binding.commentContainer, response.account!!.username!!,
                    response.content!!, response.mentions.orEmpty(), credential
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
