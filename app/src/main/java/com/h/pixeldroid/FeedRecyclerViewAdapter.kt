package com.h.pixeldroid

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL

/**
 * [RecyclerView.Adapter] that can display a list of [Post]s
 */
class FeedRecyclerViewAdapter(
    private val posts: List<Post>,
    private val context : Context
) : RecyclerView.Adapter<FeedRecyclerViewAdapter.ViewHolder>() {

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

        if(post.getProfilePicUrl() != null) {
            setImageViewFromURL(holder.postView, post.getProfilePicUrl(), holder.profilePic!!)
        }
        setImageViewFromURL(holder.postView, post.getPostUrl(), holder.postPic)
        holder.username.text = post.getUsername()
        holder.usernameDesc.text = post.getUsername()
        holder.description.text = post.getDescription()
        holder.nlikes.text = post.getNLikes()
        holder.nshares.text = post.getNShares()
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
    }
}
