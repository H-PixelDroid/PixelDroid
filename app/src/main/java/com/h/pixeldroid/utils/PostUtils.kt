package com.h.pixeldroid.utils

import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.ViewHolder
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Context
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostUtils {
    companion object {
        fun toggleCommentInput(
            holder : ViewHolder
        ) {
            //Toggle comment button
            holder.commenter.setOnClickListener {
                when(holder.commentIn.visibility) {
                    View.VISIBLE -> holder.commentIn.visibility = View.GONE
                    View.INVISIBLE -> holder.commentIn.visibility = View.VISIBLE
                    View.GONE -> holder.commentIn.visibility = View.VISIBLE
                }
            }
        }

        fun likePostCall(
            holder : ViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            api.likePost(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("LIKE ERROR", t.toString())
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown like count and internal like toggle
                        holder.nlikes.text = resp.getNLikes()
                        holder.isLiked = resp.favourited
                    } else {
                        Log.e("RESPOSE_CODE", response.code().toString())
                    }
                }

            })
        }

        fun unLikePostCall(
            holder : ViewHolder,
            api: PixelfedAPI,
            credential: String,
            post : Status
        ) {
            api.unlikePost(credential, post.id).enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("UNLIKE ERROR", t.toString())
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    if(response.code() == 200) {
                        val resp = response.body()!!

                        //Update shown like count and internal like toggle
                        holder.nlikes.text = resp.getNLikes()
                        holder.isLiked = resp.favourited
                    } else {
                        Log.e("RESPOSE_CODE", response.code().toString())
                    }

                }

            })
        }

        fun postComment(
            holder : ViewHolder,
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
                        addComment(holder.context, holder.commentCont, resp.account, resp.content)

                        Toast.makeText(holder.context,"Comment: \"$textIn\" posted!", Toast.LENGTH_SHORT).show()
                        Log.e("COMMENT SUCCESS", "posted: $textIn")
                    } else {
                        Log.e("ERROR_CODE", response.code().toString())
                    }
                }
            })
        }

        fun addComment(context: android.content.Context, commentContainer: LinearLayout, commentAccount: Account, commentContent: String) {
            //Create UI views
            val container = CardView(context)
            val layout = LinearLayout(context)
            val comment = TextView(context)
            val user = TextView(context)

            //Create comment view hierarchy
            layout.addView(user)
            layout.addView(comment)
            container.addView(layout)

            commentContainer.addView(container)

            //Set an id for the created comment (useful for testing)
            container.id = R.id.comment

            //Set overall margin
            val containerParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            containerParams.setMargins(20, 10, 20, 10)
            container.layoutParams = containerParams

            //Set layout constraints and content
            container.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layout.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            user.text = commentAccount.username
            user.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            user.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            (user.layoutParams as LinearLayout.LayoutParams).weight = 8f
            user.typeface = Typeface.DEFAULT_BOLD
            comment.text = commentContent
            comment.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            comment.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            (comment.layoutParams as LinearLayout.LayoutParams).weight = 2f
        }

        fun retrieveComments(
            holder : ViewHolder,
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
                            addComment(holder.context, holder.commentCont, status.account, status.content)
                        }
                    } else {
                        Log.e("COMMENT ERROR", "${response.code()} with body ${response.errorBody()}")
                    }
                }
            })
        }
    }
}