package com.h.pixeldroid.fragments.feeds.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.R
import com.h.pixeldroid.fragments.feeds.AccountListFragment
import com.h.pixeldroid.fragments.feeds.FeedFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Results
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchAccountFragment: AccountListFragment(){

    private lateinit var query: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        query = arguments?.getSerializable("searchFeed") as String

        return view
    }

    inner class SearchAccountListDataSource: FeedDataSource(null, null){

        override fun newSource(): FeedDataSource {
            return SearchAccountListDataSource()
        }

        private fun makeInitialCall(requestedLoadSize: Int): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken",
                    limit="$requestedLoadSize", q=query,
                    type = Results.SearchType.accounts)
        }
        private fun makeAfterCall(requestedLoadSize: Int, key: String): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken", max_id=key,
                    limit="$requestedLoadSize", q = query,
                    type = Results.SearchType.accounts)
        }
        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<Account>
        ) {
            searchEnqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Account>) {
            searchEnqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }
        private fun searchEnqueueCall(call: Call<Results>, callback: LoadCallback<Account>) {

            call.enqueue(object : Callback<Results> {
                override fun onResponse(call: Call<Results>, response: Response<Results>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!!.accounts as ArrayList<Account>
                        callback.onResult(notifications as List<Account>)

                    } else{
                        Toast.makeText(context, getString(R.string.loading_toast), Toast.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<Results>, t: Throwable) {
                    Toast.makeText(context,getString(R.string.feed_failed), Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }


    }

    override fun makeContent(): LiveData<PagedList<Account>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory =
            FeedFragment<Account, FollowsRecyclerViewAdapter.ViewHolder>()
                .FeedDataSourceFactory(
                SearchAccountListDataSource()
            )
        return LivePagedListBuilder(factory, config).build()
    }
}