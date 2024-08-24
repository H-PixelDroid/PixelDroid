package org.pixeldroid.app.directmessages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.DirectMessagesConversationItemBinding
import org.pixeldroid.app.posts.AlbumActivity
import org.pixeldroid.app.posts.AlbumViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedContentRepository
import org.pixeldroid.app.posts.feeds.cachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.cachedFeeds.ViewModelFactory
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.api.objects.Message
import org.pixeldroid.app.utils.db.entities.DirectMessageDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder


/**
 * Fragment for one Direct Messages conversation
 */
class ConversationFragment : CachedFeedFragment<DirectMessageDatabaseEntity>() {

    companion object {
        const val CONVERSATION_ID = "ConversationFragmentConversationId"
        const val PROFILE_ID = "ConversationFragmentProfileId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = DirectMessagesListAdapter(apiHolder)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.createView(inflater, container, savedInstanceState, true)

        val pid = arguments?.getSerializable(PROFILE_ID) as String
        val conversationId = arguments?.getSerializable(CONVERSATION_ID) as String

        val dao = db.directMessagesConversationDao()
        val remoteMediator = ConversationRemoteMediator(apiHolder, db, pid, conversationId)
        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelFactory(
                db, dao, remoteMediator,
                FeedContentRepository(db, dao, remoteMediator, conversationId)
            )
        )["directMessagesConversation", FeedViewModel::class.java] as FeedViewModel<DirectMessageDatabaseEntity>

        launch()
        initSearch()

        return view
    }

    /**
     * View Holder for a [Conversation] RecyclerView list item.
     */
    class DirectMessagesConversationViewHolder(val binding: DirectMessagesConversationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var message: DirectMessageDatabaseEntity? = null

        init {
            itemView.setOnClickListener {
                message?.let {
                    if (it.type == "photo") {
                        val intent = Intent(itemView.context, AlbumActivity::class.java)

                        intent.putExtra(AlbumViewModel.ALBUM_IMAGES, ArrayList(it.carousel.orEmpty()))
                        intent.putExtra(AlbumViewModel.ALBUM_INDEX, 0)

                        itemView.context.startActivity(intent)
                    }
                }
            }
        }

        fun bind(
            message: DirectMessageDatabaseEntity?,
            api: PixelfedAPIHolder,
            lifecycleScope: LifecycleCoroutineScope,
        ) {
            this.message = message

            if(message?.isAuthor == true) {
                binding.messageIncoming.visibility = GONE
                binding.messageOutgoing.visibility = VISIBLE
                binding.textMessageOutgoing.text = message.text
            } else {
                binding.messageIncoming.visibility = VISIBLE
                binding.messageOutgoing.visibility = GONE
                binding.textMessageIncoming.text = message?.text ?: ""
            }

            if (message?.type == "photo"){
                binding.imageMessageIncoming.visibility = VISIBLE
                binding.imageMessageOutgoing.visibility = VISIBLE
                binding.textMessageOutgoing.visibility = GONE
                binding.textMessageOutgoing.visibility = GONE
                Glide.with(if(message.isAuthor == true) binding.imageMessageOutgoing else binding.imageMessageIncoming)
                    .load(message.media)
                    .into(if(message.isAuthor == true) binding.imageMessageOutgoing else binding.imageMessageIncoming)
            } else {
                binding.imageMessageIncoming.visibility = GONE
                binding.imageMessageOutgoing.visibility = GONE
                binding.textMessageOutgoing.visibility = VISIBLE
                binding.textMessageIncoming.visibility = VISIBLE
            }

            message?.created_at.let {
//                if (it == null) binding.messageTime.text = ""
//                else setTextViewFromISO8601(
//                    it,
//                    binding.messageTime,
//                    false
//                )
            }
        }

        companion object {
            fun create(parent: ViewGroup): DirectMessagesConversationViewHolder {
                val itemBinding = DirectMessagesConversationItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return DirectMessagesConversationViewHolder(itemBinding)
            }
        }
    }


    inner class DirectMessagesListAdapter(
        private val apiHolder: PixelfedAPIHolder,
    ) : PagingDataAdapter<DirectMessageDatabaseEntity, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<DirectMessageDatabaseEntity>() {
            override fun areItemsTheSame(
                oldItem: DirectMessageDatabaseEntity,
                newItem: DirectMessageDatabaseEntity
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: DirectMessageDatabaseEntity,
                newItem: DirectMessageDatabaseEntity
            ): Boolean =
                oldItem == newItem
        }
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return DirectMessagesConversationViewHolder.create(parent)
        }

        override fun getItemViewType(position: Int): Int {
            return R.layout.direct_messages_conversation_item
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val uiModel = getItem(position)
            uiModel?.let {
                (holder as DirectMessagesConversationViewHolder).bind(
                    it,
                    apiHolder,
                    lifecycleScope
                )
            }
        }
    }
}