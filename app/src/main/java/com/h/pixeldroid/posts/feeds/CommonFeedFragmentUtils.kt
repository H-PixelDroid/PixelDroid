package com.h.pixeldroid.posts.feeds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.FragmentFeedBinding
import com.h.pixeldroid.databinding.LoadStateFooterViewItemBinding

/**
 * Shows or hides the error in the different FeedFragments
 */
private fun showError(errorText: String, show: Boolean = true, binding: FragmentFeedBinding){
    if(show){
        binding.motionLayout.transitionToEnd()
        binding.errorLayout.errorText.text = errorText
    } else if(binding.motionLayout.progress == 1F){
        binding.motionLayout.transitionToStart()
    }
}

/**
 * Initialises the [RecyclerView] adapter for the different FeedFragments.
 *
 * Makes the UI respond to various [LoadState]s, including errors when an error message is shown.
 */
internal fun <T: Any> initAdapter(binding: FragmentFeedBinding, adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>) {
    binding.list.adapter = adapter.withLoadStateFooter(
        footer = ReposLoadStateAdapter { adapter.retry() }
    )

    adapter.addLoadStateListener { loadState ->

        if(!binding.progressBar.isVisible && binding.swipeRefreshLayout.isRefreshing) {
            // Stop loading spinner when loading is done
            binding.swipeRefreshLayout.isRefreshing = loadState.refresh is LoadState.Loading
        } else {
            // ProgressBar should stop showing as soon as the source stops loading ("source"
            // meaning the database, so don't wait on the network)
            val sourceLoading = loadState.source.refresh is LoadState.Loading
            if(!sourceLoading && binding.list.size > 0){
                binding.list.isVisible = true
                binding.progressBar.isVisible = false
            } else if(binding.list.size ==  0
                    && loadState.append is LoadState.NotLoading
                    && loadState.append.endOfPaginationReached){
                binding.progressBar.isVisible = false
                showError(binding = binding, errorText = "Nothing to see here :(")
            }
        }


        // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
        val errorState = loadState.source.append as? LoadState.Error
            ?: loadState.source.prepend as? LoadState.Error
            ?: loadState.source.refresh as? LoadState.Error
            ?: loadState.append as? LoadState.Error
            ?: loadState.prepend as? LoadState.Error
            ?: loadState.refresh as? LoadState.Error
        errorState?.let {
            showError(binding = binding, errorText = it.error.toString())
        }
        if (errorState == null) showError(binding = binding, show = false, errorText = "")
    }

}

/**
 * Adapter to the show the a [RecyclerView] item for a [LoadState], with a callback to retry if
 * the retry button is pressed.
 */
class ReposLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<ReposLoadStateViewHolder>() {
    override fun onBindViewHolder(holder: ReposLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ReposLoadStateViewHolder {
        return ReposLoadStateViewHolder.create(parent, retry)
    }
}

/**
 * [RecyclerView.ViewHolder] that is shown at the end of the feed to indicate loading or errors
 * in the loading of appending values.
 */
class ReposLoadStateViewHolder(
    private val binding: LoadStateFooterViewItemBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
        if (loadState is LoadState.Error) {
            binding.errorMsg.text = loadState.error.localizedMessage
        }
        binding.progressBar.isVisible = loadState is LoadState.Loading
        binding.retryButton.isVisible = loadState !is LoadState.Loading
        binding.errorMsg.isVisible = loadState !is LoadState.Loading
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): ReposLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.load_state_footer_view_item, parent, false)
            val binding = LoadStateFooterViewItemBinding.bind(view)
            return ReposLoadStateViewHolder(binding, retry)
        }
    }
}