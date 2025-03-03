package org.pixeldroid.app.posts.feeds

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ErrorLayoutBinding
import org.pixeldroid.app.databinding.LoadStateFooterViewItemBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.stories.StoriesAdapter
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.insetsListener
import retrofit2.HttpException

/**
 * Shows or hides the error in the different FeedFragments
 */
private fun showError(
        errorText: String, show: Boolean = true,
        motionLayout: MotionLayout,
        errorLayout: ErrorLayoutBinding){

    if(show) {
        motionLayout.transitionToEnd()
        errorLayout.errorText.text = errorText
    } else if(motionLayout.progress == 1F) {
        motionLayout.transitionToStart()
    }
}

/**
 * Initialises the [RecyclerView] adapter for the different FeedFragments.
 *
 * Makes the UI respond to various [LoadState]s, including errors when an error message is shown.
 */
internal fun <T: Any> initAdapter(
    progressBar: ProgressBar, swipeRefreshLayout: SwipeRefreshLayout?,
    recyclerView: RecyclerView, motionLayout: MotionLayout, errorLayout: ErrorLayoutBinding,
    adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>,
    header: StoriesAdapter? = null
) {
    recyclerView.insetsListener()
//    setOnApplyWindowInsetsListener { view, insets ->
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            view.updatePadding(top = insets.getInsets(Type.systemBars()).top)
//        } else {
//            view.updatePadding(top = insets.systemWindowInsetTop)
//        }
//        insets
//    }

    val footer = ReposLoadStateAdapter { adapter.retry() }

    adapter.addLoadStateListener { loadStates: CombinedLoadStates ->
        footer.loadState = loadStates.append
    }

    recyclerView.adapter = ConcatAdapter(
        *listOfNotNull(
            header, // need to filter it if null
            adapter,
            footer
        ).toTypedArray()
    )

    swipeRefreshLayout?.setOnRefreshListener {
        adapter.refresh()
        adapter.notifyDataSetChanged()
        header?.refreshStories()
    }

    adapter.addLoadStateListener { loadState ->

        if(!progressBar.isVisible && swipeRefreshLayout?.isRefreshing == true) {
            // Stop loading spinner when loading is done
            swipeRefreshLayout.isRefreshing = loadState.refresh is LoadState.Loading
        }

        // ProgressBar should stop showing as soon as the source stops loading ("source"
        // meaning the database, so don't wait on the network)
        val sourceLoading = loadState.source.refresh is LoadState.Loading
        if (!sourceLoading && adapter.itemCount > 0) {
            recyclerView.isVisible = true
            progressBar.isVisible = false
        }

        // Show any error, regardless of whether it came from RemoteMediator or PagingSource
        val errorState = loadState.source.append as? LoadState.Error
            ?: loadState.source.prepend as? LoadState.Error
            ?: loadState.source.refresh as? LoadState.Error
            ?: loadState.append as? LoadState.Error
            ?: loadState.prepend as? LoadState.Error
            ?: loadState.refresh as? LoadState.Error

        if(errorState?.error is CancellationException){
            return@addLoadStateListener
        }

        errorState?.let {
            val error: String = (it.error as? HttpException)?.response()?.errorBody()?.string()?.ifEmpty { null }?.let { s ->
                try {
                    Gson().fromJson(s, org.pixeldroid.app.utils.api.objects.Error::class.java)?.error?.ifBlank { null }
                } catch (exception: Exception) {
                    errorLayout.root.context.getString(
                        R.string.unknown_error_in_error,
                        it.error.localizedMessage.orEmpty()
                    )
                }
            } ?: it.error.localizedMessage.orEmpty()
            showError(motionLayout = motionLayout, errorLayout = errorLayout, errorText = error)
        }

        // If the state is not an error, hide the error layout, or show message that the feed is empty
        if(errorState == null) {
            if (adapter.itemCount == 0
                && loadState.append is LoadState.NotLoading
                && loadState.append.endOfPaginationReached
            ) {
                progressBar.isVisible = false
                showError(
                    motionLayout = motionLayout, errorLayout = errorLayout,
                    errorText = errorLayout.root.context.getString(R.string.empty_feed),
                )
            } else {
                showError(motionLayout = motionLayout, errorLayout = errorLayout, show = false, errorText = "")
            }
        }
    }
}

fun <T: FeedContent> launch(
        job: Job?, lifecycleScope: LifecycleCoroutineScope, viewModel: FeedViewModel<T>,
        pagingDataAdapter: PagingDataAdapter<T, RecyclerView.ViewHolder>): Job {
    // Make sure we cancel the previous job before creating a new one
    job?.cancel()
    return lifecycleScope.launch {
        viewModel.flow.collectLatest {
            pagingDataAdapter.submitData(it)
        }
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

val UIMODEL_STATUS_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
    override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
        oldItem.id == newItem.id
}