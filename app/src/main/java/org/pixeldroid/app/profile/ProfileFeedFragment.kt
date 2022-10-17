package org.pixeldroid.app.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import org.pixeldroid.app.posts.feeds.uncachedFeeds.*
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

/**
 * Fragment to show a list of [Account]s, as a result of a search.
 */
class ProfileFeedFragment : UncachedFeedFragment<Status>() {

    private lateinit var accountId : String
    private var user: UserDatabaseEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ProfilePostsAdapter()

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
}