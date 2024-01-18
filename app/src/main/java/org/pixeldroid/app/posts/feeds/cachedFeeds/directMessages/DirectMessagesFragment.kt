package org.pixeldroid.app.posts.feeds.cachedFeeds.directMessages

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
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
import org.pixeldroid.app.databinding.FragmentConversationsBinding
import org.pixeldroid.app.databinding.FragmentNotificationsBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.ViewModelFactory
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.posts.setTextViewFromISO8601
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.notificationsWorker.makeChannelGroupId


/**
 * Fragment for the notifications tab.
 */
class DirectMessagesFragment : CachedFeedFragment<Conversation>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = DirectMessagesAdapter(apiHolder)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelFactory(db, db.directMessagesDao(), DirectMessagesRemoteMediator(apiHolder, db))
        )["conversations", FeedViewModel::class.java] as FeedViewModel<Conversation>

        launch()
        initSearch()

        return view
    }


    override fun onResume() {
        super.onResume()
        with(NotificationManagerCompat.from(requireContext())) {
            // Cancel account notification group
            db.userDao().getActiveUser()?.let {
                cancel( makeChannelGroupId(it).hashCode())
            }
        }
    }

    /**
     * View Holder for a [Conversation] RecyclerView list item.
     */
    class ConversationViewHolder(binding: FragmentConversationsBinding) : RecyclerView.ViewHolder(binding.root) {
        private val messageTime: TextView = binding.messageTime
        private val avatar: ImageView = binding.notificationAvatar
        private val photoThumbnail: ImageView = binding.notificationPhotoThumbnail

        private var conversation: Conversation? = null

        init {
            itemView.setOnClickListener {
                conversation?.openActivity()
            }
            avatar.setOnClickListener {
                val intent = conversation?.openAccountFromNotification()
                itemView.context.startActivity(intent)
            }
        }

        private fun Conversation.openActivity() {
            val intent: Intent = openConversation()
            itemView.context.startActivity(intent)
        }

        private fun Notification.openConversation(): Intent =
            Intent(itemView.context, PostActivity::class.java).apply {
                putExtra(Status.POST_TAG, status)
            }

        private fun setNotificationType(
            type: Notification.NotificationType,
            username: String,
            textView: TextView
        ) {
            val context = textView.context
            val (format: String, drawable: Drawable?) = when (type) {
                Notification.NotificationType.follow ->
                    getStringAndDrawable(
                        context,
                        R.string.followed_notification,
                        R.drawable.ic_follow
                    )
                Notification.NotificationType.mention ->
                    getStringAndDrawable(
                        context,
                        R.string.mention_notification,
                        R.drawable.mention_at_24dp
                    )
                Notification.NotificationType.comment ->
                    getStringAndDrawable(
                        context,
                        R.string.comment_notification,
                        R.drawable.ic_comment_empty
                    )
                Notification.NotificationType.reblog ->
                    getStringAndDrawable(
                        context,
                        R.string.shared_notification,
                        R.drawable.ic_reblog_blue
                    )
                Notification.NotificationType.favourite ->
                    getStringAndDrawable(
                        context,
                        R.string.liked_notification,
                        R.drawable.ic_like_full
                    )
                Notification.NotificationType.poll ->
                    getStringAndDrawable(context, R.string.poll_notification, R.drawable.poll)
                Notification.NotificationType.follow_request -> getStringAndDrawable(
                    context,
                    R.string.follow_request,
                    R.drawable.ic_follow
                )
                Notification.NotificationType.status -> getStringAndDrawable(
                    context,
                    R.string.status_notification,
                    R.drawable.ic_comment_empty
                )
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
            conversation: Conversation?,
            api: PixelfedAPIHolder,
            lifecycleScope: LifecycleCoroutineScope,
        ) {

            this.conversation = conversation

            Glide.with(itemView).load(conversation?.accounts?.first()?.anyAvatar()).circleCrop()
                .into(avatar)

            val previewUrl = conversation?.accounts?.first()?.anyAvatar()
            if (!previewUrl.isNullOrBlank()) {
                Glide.with(itemView).load(previewUrl)
                    .placeholder(R.drawable.ic_picture_fallback).into(photoThumbnail)
            } else {
                photoThumbnail.visibility = View.GONE
            }

            conversation?.last_status?.created_at?.let {
                setTextViewFromISO8601(
                    it,
                    notificationTime,
                    false
                )
            }

            // Convert HTML to clickable text
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
            fun create(parent: ViewGroup): ConversationViewHolder {
                val itemBinding = FragmentNotificationsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return ConversationViewHolder(itemBinding)
            }
        }
    }


    inner class DirectMessagesAdapter(
        private val apiHolder: PixelfedAPIHolder,
    ) : PagingDataAdapter<Conversation, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(
                oldItem: Conversation,
                newItem: Conversation
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: Conversation,
                newItem: Conversation
            ): Boolean =
                oldItem == newItem
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ConversationViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.fragment_notifications
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position)
            uiModel?.let {
                (holder as ConversationViewHolder).bind(
                        it,
                        apiHolder,
                        lifecycleScope
                )
            }
        }
    }
}