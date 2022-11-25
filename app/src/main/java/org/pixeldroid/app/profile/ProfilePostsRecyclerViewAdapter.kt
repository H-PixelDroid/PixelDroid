package org.pixeldroid.app.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.databinding.FragmentProfilePostsBinding

class ProfilePostViewHolder(val postView: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(postView.root) {
    val postPreview: ImageView = postView.postPreview
    val albumIcon: ImageView = postView.albumIcon
    val videoIcon: ImageView = postView.videoIcon

    companion object {
        fun create(parent: ViewGroup): ProfilePostViewHolder {
            val itemBinding = FragmentProfilePostsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ProfilePostViewHolder(itemBinding)
        }
    }
}