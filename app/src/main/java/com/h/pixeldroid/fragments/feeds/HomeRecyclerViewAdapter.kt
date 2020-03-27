package com.h.pixeldroid.fragments.feeds

import android.graphics.Typeface
import android.util.DisplayMetrics
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.h.pixeldroid.R
import com.h.pixeldroid.db.PostViewModel
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DatabaseUtils
import com.h.pixeldroid.utils.ImageConverter.Companion.setDefaultImage
import com.h.pixeldroid.utils.ImageConverter.Companion.setImageViewFromURL
import com.h.pixeldroid.utils.ImageConverter.Companion.setRoundImageFromURL

/**
 * [RecyclerView.Adapter] that can display a list of Posts
 */
class HomeRecyclerViewAdapter() : FeedsRecyclerViewAdapter<Status, HomeRecyclerViewAdapter.ViewHolder>() {

    private var postViewModel: PostViewModel? = null

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
        val status = feedContent[position]
        val metrics = DisplayMetrics()

        //Limit the height of the different images
        holder.profilePic?.maxHeight = metrics.heightPixels
        holder.postPic.maxHeight = metrics.heightPixels

        //Set the two images
        setRoundImageFromURL(holder.postView, status.getProfilePicUrl(), holder.profilePic!!)
        setImageViewFromURL(holder.postView, status.getPostUrl(), holder.postPic)

        //Set the image back to a placeholder if the original is too big
        if(holder.postPic.height > metrics.heightPixels) {
            setDefaultImage(holder.postView, holder.postPic)
        }

        //Set the the text views
        holder.username.text = status.getUsername()
        holder.username.setTypeface(null, Typeface.BOLD)

        holder.usernameDesc.text = status.getUsername()
        holder.usernameDesc.setTypeface(null, Typeface.BOLD)

        holder.description.text = status.getDescription()

        holder.nlikes.text = status.getNLikes()
        holder.nlikes.setTypeface(null, Typeface.BOLD)

        holder.nshares.text = status.getNShares()
        holder.nshares.setTypeface(null, Typeface.BOLD)
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
    }
}
