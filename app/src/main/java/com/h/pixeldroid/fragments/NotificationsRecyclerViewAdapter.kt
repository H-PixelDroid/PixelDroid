package com.h.pixeldroid.fragments


import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Notification
import kotlinx.android.synthetic.main.fragment_notifications.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class NotificationsRecyclerViewAdapter: RecyclerView.Adapter<NotificationsRecyclerViewAdapter.ViewHolder>() {
    private val notifications: ArrayList<Notification> = arrayListOf()
    private lateinit var context: Context

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val notification = v.tag as Notification
            openActivity(notification)
        }
    }
    private fun openActivity(notification: Notification){
        val url = notification.status?.url ?: notification.account.url
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }

    fun addNotifications(newNotifications: List<Notification>){
        val oldSize = notifications.size
        notifications.addAll(newNotifications)
        notifyItemRangeInserted(oldSize, newNotifications.size)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_notifications, parent, false)
        context = view.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        Glide.with(holder.mView).load(notification.account.avatar_static).apply(RequestOptions().circleCrop())
            .placeholder(R.drawable.ic_default_user).into(holder.avatar)

        val previewUrl = notification.status?.media_attachments?.get(0)?.preview_url
        if(!previewUrl.isNullOrBlank()){
            Glide.with(holder.mView).load(previewUrl)
                .placeholder(R.drawable.ic_picture_fallback).into(holder.photoThumbnail)
        } else{
            holder.photoThumbnail.visibility = View.GONE
        }

        setNotificationType(notification.type, notification.account.username, holder.notificationType)

        holder.postDescription.text = notification.status?.content ?: ""


        with(holder.mView) {
            tag = notification
            setOnClickListener(mOnClickListener)
        }
    }

    private fun setNotificationType(type: Notification.NotificationType, username: String,
                                    textView: TextView){
        val context = textView.context
        val drawable: Drawable?
        val format: String
        when(type) {
            Notification.NotificationType.follow -> {
                drawable = context.getDrawable(R.drawable.ic_follow)
                format = context.getString(R.string.followed_notification)
            }
            Notification.NotificationType.mention -> {
                format = context.getString(R.string.mention_notification)
                drawable = context.getDrawable(R.drawable.ic_apenstaart)
            }

            Notification.NotificationType.reblog -> {
                format = context.getString(R.string.shared_notification)
                drawable = context.getDrawable(R.drawable.ic_share)
            }

            Notification.NotificationType.favourite -> {
                format = context.getString(R.string.liked_notification)
                drawable = context.getDrawable(R.drawable.ic_heart)
            }
        }
        textView.text = format.format(username)
        textView.setCompoundDrawablesWithIntrinsicBounds(
            drawable,null,null,null
        )
    }

    override fun getItemCount(): Int = notifications.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val notificationType: TextView = mView.notification_type
        val postDescription: TextView = mView.notification_post_description
        val avatar: ImageView = mView.notification_avatar
        val photoThumbnail: ImageView = mView.notification_photo_thumbnail

        override fun toString(): String {
            return super.toString() + " '" + postDescription.text + "'"
        }
    }
}
