package com.h.pixeldroid.fragments.feeds

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.objects.Results
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFeedFragment: PostsFeedFragment(){

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

    inner class SearchFeedDataSource(
    ) : FeedDataSource(null, null){

        private fun makeInitialCall(requestedLoadSize: Int): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken",
                    limit="$requestedLoadSize", q=query,
                    type = Results.SearchType.statuses)
        }
        private fun makeAfterCall(requestedLoadSize: Int, key: String): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken", max_id=key,
                    limit="$requestedLoadSize", q = query,
                    type = Results.SearchType.statuses)
        }
        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<Status>
        ) {
            enqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Status>) {
            enqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }

        override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<Status>) {
            //do nothing here, it is expected to pull to refresh to load newer notifications
        }

        private fun enqueueCall(call: Call<Results>, callback: LoadCallback<Status>){

            call.enqueue(object : Callback<Results> {
                override fun onResponse(call: Call<Results>, response: Response<Results>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!!.statuses as ArrayList<Status>
                        callback.onResult(notifications as List<Status>)

                    } else{
                        Toast.makeText(context,"Something went wrong while loading", Toast.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<Results>, t: Throwable) {
                    Toast.makeText(context,"Could not get feed", Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }


    }

    override fun stuff() {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = SearchFeedDataSourceFactory()
        content = LivePagedListBuilder(factory, config).build()
        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })
    }

    inner class SearchFeedDataSourceFactory: FeedFragment<Status, PostViewHolder>.FeedDataSourceFactory(null, null) {

        override fun create(): DataSource<String, Status> {
            val dataSource = SearchFeedDataSource()
            liveData = MutableLiveData()
            liveData.postValue(dataSource)
            return dataSource
        }


    }


}