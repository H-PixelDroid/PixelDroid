package com.h.pixeldroid

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.wifi.hotspot2.pps.Credential
import android.util.DisplayMetrics
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setDefaultImage
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

/**
 * [RecyclerView.Adapter] that can display a list of [Post]s
 */
class FeedRecyclerViewAdapter(
    private val context : Context
) : RecyclerView.Adapter<FeedRecyclerViewAdapter.ViewHolder>() {
    private val posts: ArrayList<Post> = ArrayList<Post>()
    private lateinit var api : PixelfedAPI
    private lateinit var credential : String
    private lateinit var user : Account

    fun addPosts(newPosts : List<Post>) {
        val size = posts.size
        posts.addAll(newPosts)
        notifyItemRangeInserted(size, newPosts.size)
    }

    fun addApiAccess(pixelfedApi : PixelfedAPI, userCredentials : String, checkedUser : Account) {
        api = pixelfedApi
        credential = "Bearer $userCredentials"
        user = checkedUser
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.post_fragment, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds the different elements of the Post Model to the view holder
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        val metrics = DisplayMetrics()

        //Limit the height of the different images
        holder.profilePic?.maxHeight = metrics.heightPixels
        holder.postPic.maxHeight = metrics.heightPixels

        //Set the two images
        setRoundImageFromURL(holder.postView, post.getProfilePicUrl(), holder.profilePic!!)
        setImageViewFromURL(holder.postView, post.getPostUrl(), holder.postPic)

        //Set the image back to a placeholder if the original is too big
        if(holder.postPic.height > metrics.heightPixels) {
            setDefaultImage(holder.postView, holder.postPic)
        }

        //Set the the text views
        holder.username.text = post.getUsername()
        holder.username.setTypeface(null, Typeface.BOLD)

        holder.usernameDesc.text = post.getUsername()
        holder.usernameDesc.setTypeface(null, Typeface.BOLD)

        holder.description.text = post.getDescription()

        holder.nlikes.text = post.getNLikes()
        holder.nlikes.setTypeface(null, Typeface.BOLD)

        holder.nshares.text = post.getNShares()
        holder.nshares.setTypeface(null, Typeface.BOLD)

        //Activate the liker
        holder.liker.setOnClickListener {
            lateinit var apiAction : Call<Status>
            if(post.isLiked()) {
                apiAction = api.unlikePost(credential, post.id)
                Log.e("POST", "unLiked")
            } else {
                apiAction = api.likePost(credential, post.id)
                Log.e("POST", "Liked")
            }
            apiAction.enqueue(object : Callback<Status> {
                override fun onFailure(call: Call<Status>, t: Throwable) {
                    Log.e("Hmmmm not good", t.toString())
                }

                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    holder.nlikes.text = Post(response.body()).getNLikes()
                }

            })
        }
    }

    override fun getItemCount(): Int = posts.size

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
    }
}
