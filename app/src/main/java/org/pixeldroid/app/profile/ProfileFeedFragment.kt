package org.pixeldroid.app.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.posts.feeds.uncachedFeeds.*
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.displayDimensionsInPx

/**
 * Fragment to show a list of [Account]s, as a result of a search.
 */
class ProfileFeedFragment : UncachedFeedFragment<Status>() {

    companion object {
        const val PROFILE_GRID = "ProfileGrid"
    }

    private lateinit var accountId : String
    private var user: UserDatabaseEntity? = null
    private var grid: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        grid = arguments?.getSerializable(PROFILE_GRID) as Boolean
        adapter = ProfilePostsAdapter(grid)

        //get the currently active user
        user = db.userDao().getActiveUser()
        // Set profile according to given account
        val account = arguments?.getSerializable(Account.ACCOUNT_TAG) as Account?
        accountId = account?.id ?: user!!.user_id
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        if(grid) {
            binding.list.layoutManager = GridLayoutManager(context, 3)
        }

        // Get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(requireActivity(), ProfileViewModelFactory(
            ProfileContentRepository(
                apiHolder.setToCurrentUser(),
                accountId
            )
        )
        )["Profile", FeedViewModel::class.java] as FeedViewModel<Status>

        launch()
        initSearch()

        return view
    }

    private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
        override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
            oldItem.content == newItem.content
    }

    inner class ProfilePostsAdapter(
        private val grid: Boolean
    ) : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
        UIMODEL_COMPARATOR
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if(grid) {
                ProfilePostsViewHolder.create(parent)
            } else {
                StatusViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val post = getItem(position)

            post?.let {
                if(grid) {
                    (holder as ProfilePostsViewHolder).bind(it)
                } else {
                    (holder as StatusViewHolder).bind(it, apiHolder, db,
                        lifecycleScope, requireContext().displayDimensionsInPx())
                }
            }
        }
    }
}


class ProfileViewModelFactory @ExperimentalPagingApi constructor(
    private val searchContentRepository: UncachedContentRepository<Status>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}