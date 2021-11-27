package org.pixeldroid.app.posts.feeds.uncachedFeeds.search

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
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentTagsBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedFeedFragment
import org.pixeldroid.app.posts.feeds.uncachedFeeds.ViewModelFactory
import org.pixeldroid.app.utils.api.objects.Results
import org.pixeldroid.app.utils.api.objects.Tag
import org.pixeldroid.app.utils.api.objects.Tag.Companion.openTag

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

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity(), ViewModelFactory(
            SearchContentRepository<Tag>(
                apiHolder.setToCurrentUser(),
                Results.SearchType.hashtags,
                query
            )
        )
        )["searchHashtag", FeedViewModel::class.java] as FeedViewModel<Tag>

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
                return oldItem.name == newItem.name
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
            tag?.apply {
                openTag(itemView.context, this.name)
            }
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