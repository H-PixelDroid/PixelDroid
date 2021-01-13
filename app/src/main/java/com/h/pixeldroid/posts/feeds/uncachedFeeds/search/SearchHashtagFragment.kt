package com.h.pixeldroid.posts.feeds.uncachedFeeds.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.FragmentTagsBinding
import com.h.pixeldroid.posts.feeds.uncachedFeeds.FeedViewModel
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedFeedFragment
import com.h.pixeldroid.posts.feeds.uncachedFeeds.ViewModelFactory
import com.h.pixeldroid.utils.api.objects.Results
import com.h.pixeldroid.utils.api.objects.Tag

/**
 * Fragment to show a list of [hashtag][Tag]s, as a result of a search.
 */
class SearchHashtagFragment : UncachedFeedFragment<Tag>() {

    private lateinit var query: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HashTagAdapter()

        query = arguments?.getSerializable("searchFeed") as String

    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(
            SearchContentRepository<Tag>(
                apiHolder.setDomainToCurrentUser(db),
                Results.SearchType.hashtags,
                db.userDao().getActiveUser()!!.accessToken,
                query
            )
        )
        )
            .get(FeedViewModel::class.java) as FeedViewModel<Tag>

        launch()
        initSearch()

        return view
    }

}



class HashTagAdapter : PagingDataAdapter<Tag, RecyclerView.ViewHolder>(
    UIMODEL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return HashTagViewHolder.create(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.fragment_tags
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiModel = getItem(position)
        uiModel.let {
            (holder as HashTagViewHolder).bind(it)
        }
    }

    companion object {
        private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Tag>() {
            override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean =
                oldItem == newItem
        }
    }
}


/**
 * View Holder for a [Tag] RecyclerView list item.
 */
class HashTagViewHolder(binding: FragmentTagsBinding) : RecyclerView.ViewHolder(binding.root) {

    private val name : TextView = binding.tagName

    private var tag: Tag? = null

    init {
        itemView.setOnClickListener {
            //TODO
        }
    }


    fun bind(tag: Tag?) {

        this.tag = tag

        @SuppressLint("SetTextI18n")
        name.text = "#" + tag?.name

    }

    companion object {
        fun create(parent: ViewGroup): HashTagViewHolder {
            val itemBinding = FragmentTagsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return HashTagViewHolder(itemBinding)
        }
    }
}