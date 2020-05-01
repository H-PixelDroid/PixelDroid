package com.h.pixeldroid.fragments.feeds

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PublicTimelineFragment: PostsFeedFragment() {

    inner class SearchFeedDataSource(
    ) : FeedDataSource(null, null){

        override fun newSource(): FeedDataSource {
            return SearchFeedDataSource()
        }

        private fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return pixelfedAPI.timelinePublic(limit="$requestedLoadSize")
        }
        private fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI.timelinePublic( max_id=key, limit="$requestedLoadSize")
        }

        private fun innerEnqueueCall(call: Call<List<Status>>, callback: LoadCallback<Status>){

            call.enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!! as ArrayList<Status>
                        callback.onResult(notifications as List<Status>)

                    } else{
                        Log.e("FeedFragment", "got response code ${response.code()}")
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("FeedFragment", t.toString())
                }
            })
        }

        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<Status>
        ) {
            innerEnqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Status>) {
            innerEnqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }
    }

    override fun makeContent(): LiveData<PagedList<Status>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = FeedFragment<Status, PostViewHolder>()
            .FeedDataSourceFactory(SearchFeedDataSource())
        return LivePagedListBuilder(factory, config).build()
    }
}