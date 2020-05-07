package com.h.pixeldroid.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.PostViewHolder
import com.h.pixeldroid.objects.Context
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageFromDrawable
import kotlinx.android.synthetic.main.comment.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class PostUtils {
    companion object {
        fun toggleCommentInput(
            holder : PostViewHolder
        ) {
            //Toggle comment button
            holder.commenter.setOnClickListener {
                when(holder.commentIn.visibility) {
                    View.VISIBLE -> {
                        holder.commentIn.visibility = View.GONE
                        setImageFromDrawable(holder.postView, holder.commenter, R.drawable.ic_comment_empty)
                    }
                    View.GONE -> {
                        holder.commentIn.visibility = View.VISIBLE
                        setImageFromDrawable(holder.postView, holder.commenter, R.drawable.ic_comment_blue)
                    }
                }
            }
        }

        fun reblogPost(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            //Call the api function
            api.reblogStatus(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("REBLOG ERROR", t.toString())
                    holder.reblogger.isChecked = false
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown share count
                        holder.nshares.text = resp.getNShares()
                        holder.reblogger.isChecked = resp.reblogged
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.reblogger.isChecked = false
                    }
                }

            })
        }

        fun undoReblogPost(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            //Call the api function
            api.undoReblogStatus(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("REBLOG ERROR", t.toString())
                    holder.reblogger.isChecked = true
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown share count
                        holder.nshares.text = resp.getNShares()
                        holder.reblogger.isChecked = resp.reblogged
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.reblogger.isChecked = true
                    }
                }

            })
        }

        fun likePostCall(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            //Call the api function
            api.likePost(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("LIKE ERROR", t.toString())
                    holder.liker.isChecked = false
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown like count and internal like toggle
                        holder.nlikes.text = resp.getNLikes()
                        holder.liker.isChecked = resp.favourited
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.liker.isChecked = false
                    }
                }

            })
        }

        fun unLikePostCall(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            //Call the api function
            api.unlikePost(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("UNLIKE ERROR", t.toString())
                    holder.liker.isChecked = true
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown like count and internal like toggle
                        holder.nlikes.text = resp.getNLikes()
                        holder.liker.isChecked = resp.favourited
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.liker.isChecked = true
                    }

                }

            })
        }

        fun bookmarkPostCall(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            // Call api function
            api.bookmarkStatus(post.id, credential).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("BOOKMARK ERROR", t.toString())
                    holder.bookmarker.isChecked = false
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        // Update post bookmarked state
                        holder.bookmarker.isChecked = resp.bookmarked
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.bookmarker.isChecked = false
                    }
                }

            })
        }

        fun unBookmarkPostCall(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            // Call api function
            api.undoBookmarkStatus(post.id, credential).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("UNBOOKMARK ERROR", t.toString())
                    holder.bookmarker.isChecked = true
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        // Update post bookmarked state
                        holder.bookmarker.isChecked = resp.bookmarked
                    } else {
                        Log.e("RESPONSE_CODE", response.code().toString())
                        holder.bookmarker.isChecked = true
                    }
                }

            })
        }

        fun postComment(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            val textIn = holder.comment.text
            val nonNullText = textIn.toString()
            api.postStatus(credential, nonNullText, post.id).enqueue(object :
                Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("COMMENT ERROR", t.toString())
                    Toast.makeText(holder.context,"Comment error!", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    //Check that the received response code is valid
                    if(response.code() == 200) {
                        val resp = response.body()!!
                        holder.commentIn.visibility = View.GONE

                        //Add the comment to the comment section
                        addComment(holder.context, holder.commentCont, resp.account.username, resp.content)

                        Toast.makeText(holder.context,"Comment: \"$textIn\" posted!", Toast.LENGTH_SHORT).show()
                        Log.e("COMMENT SUCCESS", "posted: $textIn")
                    } else {
                        Log.e("ERROR_CODE", response.code().toString())
                    }
                }
            })
        }

        fun addComment(context: android.content.Context, commentContainer: LinearLayout, commentUsername: String, commentContent: String) {

            val view = LayoutInflater.from(context)
                .inflate(R.layout.comment, commentContainer, true)

            view.user.text = commentUsername
            view.commentText.text = commentContent
        }

        fun retrieveComments(
            holder : PostViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            api.statusComments(post.id, credential).enqueue(object :
                Callback<Context> {
                override fun onFailure(call: Call<Context>, t: Throwable) {
                    Log.e("COMMENT FETCH ERROR", t.toString())
                }

                override fun onResponse(
                    call: Call<Context>,
                    response: Response<Context>
                ) {
                    if(response.code() == 200) {
                        val statuses = response.body()!!.descendants

                        //Create the new views for each comment
                        for (status in statuses) {
                            addComment(holder.context, holder.commentCont, status.account.username, status.content)
                        }
                    } else {
                        Log.e("COMMENT ERROR", "${response.code()} with body ${response.errorBody()}")
                    }
                }
            })
        }
    }
}