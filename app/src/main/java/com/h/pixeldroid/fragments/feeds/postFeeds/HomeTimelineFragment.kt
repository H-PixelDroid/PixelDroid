package com.h.pixeldroid.fragments.feeds.postFeeds

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeTimelineFragment: PostsFeedFragment() {

    override fun makeContent(): LiveData<PagedList<Status>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = PostFeedDataSource()
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder(factory, config).build()
    }


    inner class PostFeedDataSource: FeedDataSource<String, Status>() {

        override fun newSource(): PostFeedDataSource {
            return PostFeedDataSource()
        }

        override fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", limit="$requestedLoadSize")
        }

        override fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", max_id=key,
                    limit="$requestedLoadSize")
        }

        //We use the id as the key
        override fun getKey(item: Status): String {
            return item.id!!
        }

        override fun enqueueCall(call: Call<List<Status>>, callback: LoadCallback<Status>){

            call.enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val notifications = response.body()!!
                        callback.onResult(notifications)
                        DBUtils.storePosts(db, notifications, user!!)
                    } else{
                        Toast.makeText(context, getString(R.string.loading_toast), Toast.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Toast.makeText(context, getString(R.string.feed_failed), Toast.LENGTH_SHORT).show()
                    Log.e("PostsFeedFragment", t.toString())
                }
            })
        }
    }
}