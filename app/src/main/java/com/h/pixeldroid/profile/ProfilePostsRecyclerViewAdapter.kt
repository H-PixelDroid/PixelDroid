package com.h.pixeldroid.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.posts.PostActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.ImageConverter.Companion.setSquareImageFromDrawable
import com.h.pixeldroid.utils.ImageConverter.Companion.setSquareImageFromURL

/**
 * [RecyclerView.Adapter] that can display a list of [Status]s
 */
class ProfilePostsRecyclerViewAdapter: RecyclerView.Adapter<ProfilePostViewHolder>() {
    private val posts: ArrayList<Status> = ArrayList()

    fun addPosts(newPosts : List<Status>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_profile_posts, parent, false)
        return ProfilePostViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        val post = posts[position]

        if (post.sensitive!!)
            setSquareImageFromDrawable(holder.postView, getDrawable(holder.postView.context, R.drawable.ic_sensitive), holder.postPreview)
        else
            setSquareImageFromURL(holder.postView, post.getPostPreviewURL(), holder.postPreview)

        if(post.media_attachments?.size ?: 0 > 1){
            holder.albumIcon.visibility = View.VISIBLE
        } else {
            holder.albumIcon.visibility = View.GONE
        }

        holder.postPreview.setOnClickListener {
            val intent = Intent(holder.postPreview.context, PostActivity::class.java)
            intent.putExtra(Status.POST_TAG, post)
            holder.postPreview.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = posts.size
}

class ProfilePostViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
    val postPreview: ImageView = postView.findViewById(R.id.postPreview)
    val albumIcon: ImageView = postView.findViewById(R.id.albumIcon)
}