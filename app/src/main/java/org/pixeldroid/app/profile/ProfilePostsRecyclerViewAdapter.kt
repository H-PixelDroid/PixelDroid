package org.pixeldroid.app.profile

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R

class ProfilePostViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
    val postPreview: ImageView = postView.findViewById(R.id.postPreview)
    val albumIcon: ImageView = postView.findViewById(R.id.albumIcon)
}