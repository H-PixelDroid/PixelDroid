package org.pixeldroid.app.posts.feeds.cachedFeeds.notifications

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
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentNotificationsBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.ViewModelFactory
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.posts.setTextViewFromISO8601
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.di.PixelfedAPIHolder


/**
 * Fragment for the notifications tab.
 */
class NotificationsFragment : CachedFeedFragment<Notification>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NotificationsAdapter(apiHolder)
    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelFactory(db, db.notificationDao(), NotificationsRemoteMediator(apiHolder, db))
        )
            .get("notifications", FeedViewModel::class.java) as FeedViewModel<Notification>

        launch()
        initSearch()

        return view
    }


    /**
     * View Holder for a [Notification] RecyclerView list item.
     */
    class NotificationViewHolder(binding: FragmentNotificationsBinding) : RecyclerView.ViewHolder(binding.root) {
        private val notificationType: TextView = binding.notificationType
        private val notificationTime: TextView = binding.notificationTime
        private val postDescription: TextView = binding.notificationPostDescription
        private val avatar: ImageView = binding.notificationAvatar
        private val photoThumbnail: ImageView = binding.notificationPhotoThumbnail

        private var notification: Notification? = null

        init {
            itemView.setOnClickListener {
                notification?.openActivity()
            }
            avatar.setOnClickListener {
                val intent = notification?.openAccountFromNotification()
                itemView.context.startActivity(intent)
            }
        }

        private fun Notification.openActivity() {
            val intent: Intent =
                when (type) {
                    Notification.NotificationType.mention, Notification.NotificationType.favourite,
                    Notification.NotificationType.poll, Notification.NotificationType.reblog -> {
                        openPostFromNotification()
                    }
                    Notification.NotificationType.follow -> {
                        openAccountFromNotification()
                    }
                    null -> return
                }
            itemView.context.startActivity(intent)
        }

        private fun Notification.openPostFromNotification(): Intent =
            Intent(itemView.context, PostActivity::class.java).apply {
                putExtra(Status.POST_TAG, status)
            }

        private fun Notification.openAccountFromNotification(): Intent =
            Intent(itemView.context, ProfileActivity::class.java).apply {
                putExtra(Account.ACCOUNT_TAG, account)
            }


        private fun setNotificationType(
            type: Notification.NotificationType,
            username: String,
            textView: TextView
        ) {
            val context = textView.context
            val (format: String, drawable: Drawable?) = when (type) {
                Notification.NotificationType.follow -> {
                    getStringAndDrawable(
                        context,
                        R.string.followed_notification,
                        R.drawable.ic_follow
                    )
                }
                Notification.NotificationType.mention -> {
                    getStringAndDrawable(
                        context,
                        R.string.mention_notification,
                        R.drawable.mention_at_24dp
                    )
                }

                Notification.NotificationType.reblog -> {
                    getStringAndDrawable(
                        context,
                        R.string.shared_notification,
                        R.drawable.ic_reblog_blue
                    )
                }

                Notification.NotificationType.favourite -> {
                    getStringAndDrawable(
                        context,
                        R.string.liked_notification,
                        R.drawable.ic_like_full
                    )
                }
                Notification.NotificationType.poll -> {
                    getStringAndDrawable(context, R.string.poll_notification, R.drawable.poll)
                }
            }
            textView.text = format.format(username)
            textView.setCompoundDrawablesWithIntrinsicBounds(
                drawable, null, null, null
            )
        }

        private fun getStringAndDrawable(
            context: Context,
            stringToFormat: Int,
            drawable: Int
        ): Pair<String, Drawable?> =
            Pair(context.getString(stringToFormat), ContextCompat.getDrawable(context, drawable))


        fun bind(
            notification: Notification?,
            api: PixelfedAPIHolder,
            lifecycleScope: LifecycleCoroutineScope,
        ) {

            this.notification = notification

            Glide.with(itemView).load(notification?.account?.anyAvatar()).circleCrop()
                .into(avatar)

            val previewUrl = notification?.status?.media_attachments?.getOrNull(0)?.preview_url
            if (!previewUrl.isNullOrBlank()) {
                Glide.with(itemView).load(previewUrl)
                    .placeholder(R.drawable.ic_picture_fallback).into(photoThumbnail)
            } else {
                photoThumbnail.visibility = View.GONE
            }

            notification?.type?.let {
                notification.account?.username?.let { username ->
                    setNotificationType(
                        it,
                        username,
                        notificationType
                    )
                }
            }
            notification?.created_at?.let {
                setTextViewFromISO8601(
                    it,
                    notificationTime,
                    false,
                    itemView.context
                )
            }

            //Convert HTML to clickable text
            postDescription.text =
                parseHTMLText(
                        notification?.status?.content ?: "",
                        notification?.status?.mentions,
                        api,
                        itemView.context,
                        lifecycleScope
                )
        }

        companion object {
            fun create(parent: ViewGroup): NotificationViewHolder {
                val itemBinding = FragmentNotificationsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return NotificationViewHolder(itemBinding)
            }
        }
    }


    inner class NotificationsAdapter(
        private val apiHolder: PixelfedAPIHolder,
    ) : PagingDataAdapter<Notification, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Notification>() {
            override fun areItemsTheSame(
                oldItem: Notification,
                newItem: Notification
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: Notification,
                newItem: Notification
            ): Boolean =
                oldItem == newItem
        }
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
                (holder as NotificationViewHolder).bind(
                        it,
                        apiHolder,
                        lifecycleScope
                )
            }
        }
    }
}