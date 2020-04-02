package com.h.pixeldroid.fragments.feeds

import android.content.Intent
import android.graphics.Typeface

import android.util.DisplayMetrics
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.textfield.TextInputEditText
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.openProfile
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageFromDrawable
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
import kotlinx.android.synthetic.main.nav_header.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class HomeRecyclerViewAdapter(private val api: PixelfedAPI, private val credential : String) : FeedsRecyclerViewAdapter<Status, HomeRecyclerViewAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_fragment, parent, false)
        context = view.context
        return ViewHolder(view)
    }

    /**
     * Binds the different elements of the Post Model to the view holder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = feedContent[position]
        val metrics = DisplayMetrics()

        //Limit the height of the different images
        holder.profilePic?.maxHeight = metrics.heightPixels
        holder.profilePic?.setOnClickListener{ openProfile(context, post.account) }
        holder.postPic.maxHeight = metrics.heightPixels

        //Set the two images
        setRoundImageFromURL(holder.postView, post.getProfilePicUrl(), holder.profilePic!!)
        setImageViewFromURL(holder.postView, post.getPostUrl(), holder.postPic)

        //Set the image back to a placeholder if the original is too big
        if(holder.postPic.height > metrics.heightPixels) {
            setImageFromDrawable(holder.postView, holder.postPic, R.drawable.ic_picture_fallback)
        }

        //Set the the text views
        holder.username.text = post.getUsername()
        holder.username.setTypeface(null, Typeface.BOLD)
        holder.username.setOnClickListener{ openProfile(context, post.account) }

        holder.usernameDesc.text = post.getUsername()
        holder.usernameDesc.setTypeface(null, Typeface.BOLD)

        holder.description.text = post.getDescription()

        holder.nlikes.text = post.getNLikes()
        holder.nlikes.setTypeface(null, Typeface.BOLD)

        holder.nshares.text = post.getNShares()
        holder.nshares.setTypeface(null, Typeface.BOLD)

        //Activate the liker
        holder.liker.setOnClickListener {
            if (post.favourited) {
                api.unlikePost(credential, post.id).enqueue(object : Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("UNLIKE ERROR", t.toString())
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        if(response.code() == 200) {
                            val resp = response.body()!!
                            holder.nlikes.text = resp.getNLikes()
                            Log.e("POST", "unLiked")
                            Log.e("isLIKE", post.favourited.toString())

                        } else {
                            Log.e("RESPOSE_CODE", response.code().toString())
                        }

                    }

                })

            } else {
                api.likePost(credential, post.id).enqueue(object : Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("LIKE ERROR", t.toString())
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        if(response.code() == 200) {
                            val resp = response.body()!!
                            holder.nlikes.text = resp.getNLikes()
                            Log.e("POST", "liked")
                            Log.e("isLIKE", post.favourited.toString())
                        } else {
                            Log.e("RESPOSE_CODE", response.code().toString())
                        }
                    }

                })

            }
        }

        //Show all comments of a post
        api.statusComments(post.id, credential).enqueue(object : Callback<com.h.pixeldroid.objects.Context> {
            override fun onFailure(call: Call<com.h.pixeldroid.objects.Context>, t: Throwable) {
                Log.e("COMMENT FETCH ERROR", t.toString())
            }

            override fun onResponse(
                call: Call<com.h.pixeldroid.objects.Context>,
                response: Response<com.h.pixeldroid.objects.Context>
            ) {
                if(response.code() == 200) {
                    val statuses = response.body()!!.descendants
                    for (status in statuses) {
                        val container = CardView(context)
                        //Retrieve the username and comment
                        val user = TextView(context)
                        user.text = status.account.username
                        user.setTypeface(null, Typeface.BOLD)
                        val comment = TextView(context)
                        comment.text = status.text!!

                        //Fill out the container and add it to our viewHolder
                        //container.addView(user)
                        container.addView(comment)
                        holder.commentCont.addView(container)
                    }
                } else {
                    Log.e("COMMENT ERROR", "${response.code().toString()} with body ${response.errorBody()}")
                }
            }

        })

        //Activate commenter
        holder.commenter.setOnClickListener {
            Log.e("ID", post.id)
            Log.e("ACCESS_TOKEN", credential)
            //Open text input
            val textIn = holder.comment.textView?.text
            if(textIn.isNullOrEmpty()) {
                Toast.makeText(context,"Comment must not be empty!",Toast.LENGTH_SHORT).show()
            } else {
                val nonNullText = textIn.toString()
                api.commentStatus(credential, nonNullText, post.id).enqueue(object : Callback<Status> {
                    override fun onFailure(call: Call<Status>, t: Throwable) {
                        Log.e("COMMENT ERROR", t.toString())
                        Toast.makeText(context,"Comment error!",Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<Status>, response: Response<Status>) {
                        if(response.code() == 200) {

                            val comment = TextView(context)
                            comment.text = response.body()!!.text.toString()
                            holder.commentCont.addView(comment)
                            Toast.makeText(context,"Comment posted!",Toast.LENGTH_SHORT).show()
                            Log.e("COMMENT SUCCESS", "posted: ${textIn}")
                        } else {
                            Log.e("ERROR_CODE", response.code().toString())
                        }
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int = feedContent.size

    /**
     * Represents the posts that will be contained within the feed
     */
    inner class ViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
        val profilePic  : ImageView? = postView.findViewById(R.id.profilePic)
        val postPic     : ImageView = postView.findViewById(R.id.postPicture)
        val username    : TextView  = postView.findViewById(R.id.username)
        val usernameDesc: TextView  = postView.findViewById(R.id.usernameDesc)
        val description : TextView  = postView.findViewById(R.id.description)
        val nlikes      : TextView  = postView.findViewById(R.id.nlikes)
        val nshares     : TextView  = postView.findViewById(R.id.nshares)
        val liker       : ImageView = postView.findViewById(R.id.liker)
        val commenter   : ImageView = postView.findViewById(R.id.commenter)
        val comment     : TextInputEditText = postView.findViewById(R.id.editComment)
        val commentCont : LinearLayout = postView.findViewById(R.id.commentContainer)
    }
}