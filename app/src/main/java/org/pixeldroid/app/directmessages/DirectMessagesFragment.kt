package org.pixeldroid.app.directmessages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.DirectMessagesListItemBinding
import org.pixeldroid.app.directmessages.ConversationFragment.Companion.CONVERSATION_ID
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.ViewModelFactory
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.posts.setTextViewFromISO8601
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.di.PixelfedAPIHolder


/**
 * Fragment for the list of Direct Messages conversations.
 */
class DirectMessagesFragment : CachedFeedFragment<Conversation>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = DirectMessagesListAdapter(apiHolder)
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
        )["directMessagesList", FeedViewModel::class.java] as FeedViewModel<Conversation>

        launch()
        initSearch()

        return view
    }

    /**
     * View Holder for a [Conversation] RecyclerView list item.
     */
    class DirectMessagesListViewHolder(val binding: DirectMessagesListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var conversation: Conversation? = null

        init {
            itemView.setOnClickListener {
                conversation?.accounts?.firstOrNull()?.let {
                    val intent = Intent(itemView.context, ConversationActivity::class.java).apply {
                        putExtra(CONVERSATION_ID, it.id)
                    }
                    itemView.context.startActivity(intent)
                }
            }
            binding.dmAvatar.setOnClickListener {
                conversation?.accounts?.firstOrNull()?.let {
                    val intent = Intent(itemView.context, ProfileActivity::class.java).apply {
                        putExtra(Account.ACCOUNT_TAG, it)
                    }
                    itemView.context.startActivity(intent)
                }
            }
        }

        fun bind(
            conversation: Conversation?,
            api: PixelfedAPIHolder,
            lifecycleScope: LifecycleCoroutineScope,
        ) {

            this.conversation = conversation

            val account = conversation?.accounts?.firstOrNull()

            Glide.with(itemView).load(account?.anyAvatar()).circleCrop()
                .into(binding.dmAvatar)

            binding.dmUsername.text = account?.getDisplayName()

            binding.dmLastMessage.text = parseHTMLText(
                conversation?.last_status?.content ?: "",
                conversation?.last_status?.mentions,
                api,
                itemView.context,
                lifecycleScope
            )

            conversation?.last_status?.created_at.let {
                if (it == null) binding.messageTime.text = ""
                else setTextViewFromISO8601(
                    it,
                    binding.messageTime,
                    false
                )
            }
        }

        companion object {
            fun create(parent: ViewGroup): DirectMessagesListViewHolder {
                val itemBinding = DirectMessagesListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return DirectMessagesListViewHolder(itemBinding)
            }
        }
    }


    inner class DirectMessagesListAdapter(
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
            return DirectMessagesListViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.direct_messages_list_item
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position)
            uiModel?.let {
                (holder as DirectMessagesListViewHolder).bind(
                        it,
                        apiHolder,
                        lifecycleScope
                )
            }
        }
    }
}