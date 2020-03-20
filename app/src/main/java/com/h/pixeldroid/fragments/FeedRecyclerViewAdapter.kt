package com.h.pixeldroid.fragments

import android.content.Context
import android.graphics.Typeface
import android.util.DisplayMetrics
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.h.pixeldroid.R
import com.h.pixeldroid.db.PostEntity
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.utils.ImageConverter.Companion.setDefaultImage
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL
import java.util.ArrayList

/**
 * [RecyclerView.Adapter] that can display a list of [Post]s
 */
class FeedRecyclerViewAdapter(
    private val context : Context
) : RecyclerView.Adapter<FeedRecyclerViewAdapter.ViewHolder>() {

    private var posts: ArrayList<Post> = ArrayList()
    private var postsEnt = emptyList<PostEntity>() // Cached copy of words

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
    }

    override fun getItemCount(): Int = posts.size

    internal fun setPosts(posts: List<PostEntity>) {
        this.postsEnt = posts

        this.posts.clear()
        this.postsEnt.forEach(){
            this.posts.add(Post(it))
        }
        notifyDataSetChanged()

        val size = posts.size
        notifyItemRangeInserted(size, posts.size)
    }

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
    }
}
