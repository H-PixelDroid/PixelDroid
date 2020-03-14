package com.h.pixeldroid.fragments

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.h.pixeldroid.R


import com.h.pixeldroid.fragments.dummy.DummyContent.DummyItem
import com.h.pixeldroid.objects.Notification

import kotlinx.android.synthetic.main.fragment_notifications.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyNotificationsRecyclerViewAdapter(
    private val notifications: List<Notification>
) : RecyclerView.Adapter<MyNotificationsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            v.tag as DummyItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_notifications, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        Glide.with(holder.mView).load(notification.account.avatar_static)
            .placeholder(R.drawable.ic_default_user).into(holder.avatar)
        holder.notificationType.text =
            "${notification.account.username} ${when(notification.type){
                Notification.NotificationType.follow -> "followed you"
                Notification.NotificationType.mention -> "mentioned you"
                Notification.NotificationType.reblog -> "shared your post"
                Notification.NotificationType.favourite -> "liked your post"
                Notification.NotificationType.poll -> "'s poll has ended"
            }}"
        holder.postDescription.text = notification.status?.text ?: ""

        with(holder.mView) {
            tag = notification
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = notifications.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val notificationType: TextView = mView.notification_type
        val postDescription: TextView = mView.notification_post_description
        val avatar: ImageView = mView.notification_avatar

        override fun toString(): String {
            return super.toString() + " '" + postDescription.text + "'"
        }
    }
}
