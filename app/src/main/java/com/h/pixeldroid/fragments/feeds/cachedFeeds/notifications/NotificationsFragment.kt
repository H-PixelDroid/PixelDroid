package com.h.pixeldroid.fragments.feeds.cachedFeeds.notifications

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.di.PixelfedAPIHolder
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Notification
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.Utils.Companion.setTextViewFromISO8601
import kotlinx.android.synthetic.main.fragment_notifications.view.*

import com.h.pixeldroid.fragments.feeds.cachedFeeds.CachedFeedFragment
import com.h.pixeldroid.fragments.feeds.cachedFeeds.FeedViewModel
import com.h.pixeldroid.fragments.feeds.cachedFeeds.ViewModelFactory
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText


/**
 * Fragment for the notifications tab.
 */
class NotificationsFragment : CachedFeedFragment<Notification>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NotificationsAdapter(apiHolder, db)
    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(db, db.notificationDao(), NotificationsRemoteMediator(apiHolder, db)))
            .get(FeedViewModel::class.java) as FeedViewModel<Notification>

        launch()
        initSearch()

        return view
    }

}

/**
 * View Holder for a [Notification] RecyclerView list item.
 */
class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val notificationType: TextView = view.notification_type
    private val notificationTime: TextView = view.notification_time
    private val postDescription: TextView = view.notification_post_description
    private val avatar: ImageView = view.notification_avatar
    private val photoThumbnail: ImageView = view.notification_photo_thumbnail

    private var notification: Notification? = null

    init {
        itemView.setOnClickListener {
                notification?.openActivity()
            }
        }

    private fun Notification.openActivity() {
        val intent: Intent
        when (type){
            Notification.NotificationType.mention, Notification.NotificationType.favourite,
            Notification.NotificationType.poll, Notification.NotificationType.reblog -> {
                intent = openPostFromNotification()
            }
            Notification.NotificationType.follow -> {
                intent = Intent(itemView.context, ProfileActivity::class.java)
                intent.putExtra(Account.ACCOUNT_TAG, account)
            }
        }
        itemView.context.startActivity(intent)
    }

    private fun Notification.openPostFromNotification(): Intent {
        val intent = Intent(itemView.context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, status)
        return  intent
    }


    private fun setNotificationType(type: Notification.NotificationType, username: String,
                                    textView: TextView
    ){
        val context = textView.context
        val (format: String, drawable: Drawable?) = when(type) {
            Notification.NotificationType.follow -> {
                setNotificationTypeTextView(context, R.string.followed_notification, R.drawable.ic_follow)
            }
            Notification.NotificationType.mention -> {
                setNotificationTypeTextView(context, R.string.mention_notification, R.drawable.ic_apenstaart)
            }

            Notification.NotificationType.reblog -> {
                setNotificationTypeTextView(context, R.string.shared_notification, R.drawable.ic_reblog_blue)
            }

            Notification.NotificationType.favourite -> {
                setNotificationTypeTextView(context, R.string.liked_notification, R.drawable.ic_like_full)
            }
            Notification.NotificationType.poll -> {
                setNotificationTypeTextView(context, R.string.poll_notification, R.drawable.poll)
            }
        }
        textView.text = format.format(username)
        textView.setCompoundDrawablesWithIntrinsicBounds(
            drawable,null,null,null
        )
    }
    private fun setNotificationTypeTextView(context: Context, format: Int, drawable: Int): Pair<String, Drawable?> {
        return Pair(context.getString(format), ContextCompat.getDrawable(context, drawable))
    }



    fun bind(notification: Notification?, api: PixelfedAPI, accessToken: String) {

        this.notification = notification

        Glide.with(itemView).load(notification?.account?.avatar_static).circleCrop().into(avatar)

        val previewUrl = notification?.status?.media_attachments?.getOrNull(0)?.preview_url
        if(!previewUrl.isNullOrBlank()){
            Glide.with(itemView).load(previewUrl)
                .placeholder(R.drawable.ic_picture_fallback).into(photoThumbnail)
        } else{
            photoThumbnail.visibility = View.GONE
        }

        notification?.type?.let { setNotificationType(it, notification.account.username!!, notificationType) }
        notification?.created_at?.let { setTextViewFromISO8601(it, notificationTime, false, itemView.context) }

        //Convert HTML to clickable text
        postDescription.text =
            parseHTMLText(
                notification?.status?.content ?: "",
                notification?.status?.mentions,
                api,
                itemView.context,
                "Bearer $accessToken"
            )
    }

    companion object {
        fun create(parent: ViewGroup): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_notifications, parent, false)
            return NotificationViewHolder(view)
        }
    }
}


class NotificationsAdapter(private val apiHolder: PixelfedAPIHolder, private val db: AppDatabase) : PagingDataAdapter<Notification, RecyclerView.ViewHolder>(
    UIMODEL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NotificationViewHolder.create(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.fragment_notifications
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiModel = getItem(position)
        uiModel.let {
            (holder as NotificationViewHolder).bind(it, apiHolder.setDomainToCurrentUser(db), db.userDao().getActiveUser()!!.accessToken)
        }
    }

    companion object {
        private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Notification>() {
            override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean =
                oldItem == newItem
        }
    }
}