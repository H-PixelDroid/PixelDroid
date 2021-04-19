package com.h.pixeldroid.posts.feeds.uncachedFeeds.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import com.h.pixeldroid.posts.feeds.uncachedFeeds.*
import com.h.pixeldroid.posts.feeds.uncachedFeeds.accountLists.AccountAdapter
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Results

/**
 * Fragment to show a list of [Account]s, as a result of a search.
 */
class SearchAccountFragment : UncachedFeedFragment<Account>() {

    private lateinit var query: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = AccountAdapter()

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
                SearchContentRepository<Account>(
                    apiHolder.setDomainToCurrentUser(db),
                    Results.SearchType.accounts,
                    query
                )
            )
        ).get(FeedViewModel::class.java) as FeedViewModel<Account>

        launch()
        initSearch()

        return view
    }

}