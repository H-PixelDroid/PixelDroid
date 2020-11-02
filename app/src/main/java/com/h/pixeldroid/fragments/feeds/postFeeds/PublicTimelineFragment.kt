package com.h.pixeldroid.fragments.feeds.postFeeds

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PublicTimelineFragment: PostsFeedFragment() {

    inner class PublicFeedDataSource : FeedDataSource<String, Status>(){

        override fun newSource(): PublicFeedDataSource {
            return PublicFeedDataSource()
        }

        override fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return pixelfedAPI.timelinePublic(limit="$requestedLoadSize")
        }
        override fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI.timelinePublic( max_id=key, limit="$requestedLoadSize")
        }

        override fun enqueueCall(call: Call<List<Status>>, callback: LoadCallback<Status>) {
            call.enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val notifications = response.body()!!
                        callback.onResult(notifications)
                    } else{
                        showError()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    showError(errorText = R.string.feed_failed)
                    Log.e("PublicTimelineFragment", t.toString())
                }
            })
        }

        override fun getKey(item: Status): String {
            return item.id!!
        }
    }

    override fun makeContent(): LiveData<PagedList<Status>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = FeedDataSourceFactory(PublicFeedDataSource())
        return LivePagedListBuilder(factory, config).build()
    }
}