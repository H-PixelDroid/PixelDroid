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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_TAG
import com.h.pixeldroid.objects.Notification
import kotlinx.android.synthetic.main.fragment_notifications.view.*

/**
 * [RecyclerView.Adapter] that can display a [Notification]
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
        val intent: Intent
        when (notification.type){
            Notification.NotificationType.mention, Notification.NotificationType.favourite-> {
                intent = Intent(context, PostActivity::class.java)
                intent.putExtra(POST_TAG, Post(notification.status))
            }
            Notification.NotificationType.reblog-> {
                Toast.makeText(context,"Can't see shares yet, sorry!",Toast.LENGTH_SHORT).show()
                return
            }
            Notification.NotificationType.follow -> {
                val url = notification.status?.url ?: notification.account.url
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
        }
        context.startActivity(intent)
    }

    fun initializeWith(newNotifications: List<Notification>){
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
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
        val (format: String, drawable: Drawable?) = when(type) {
            Notification.NotificationType.follow -> {
                setNotificationTypeTextView(context, R.string.followed_notification, R.drawable.ic_follow)
            }
            Notification.NotificationType.mention -> {
                setNotificationTypeTextView(context, R.string.mention_notification, R.drawable.ic_apenstaart)
            }

            Notification.NotificationType.reblog -> {
                setNotificationTypeTextView(context, R.string.shared_notification, R.drawable.ic_share)
            }

            Notification.NotificationType.favourite -> {
                setNotificationTypeTextView(context, R.string.liked_notification, R.drawable.ic_heart)
            }
        }
        textView.text = format.format(username)
        textView.setCompoundDrawablesWithIntrinsicBounds(
            drawable,null,null,null
        )
    }
    private fun setNotificationTypeTextView(context: Context, format: Int, drawable: Int): Pair<String, Drawable?> {
        return Pair(context.getString(format), context.getDrawable(drawable))
    }

    override fun getItemCount(): Int = notifications.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val notificationType: TextView = mView.notification_type
        val postDescription: TextView = mView.notification_post_description
        val avatar: ImageView = mView.notification_avatar
        val photoThumbnail: ImageView = mView.notification_photo_thumbnail
    }
}
