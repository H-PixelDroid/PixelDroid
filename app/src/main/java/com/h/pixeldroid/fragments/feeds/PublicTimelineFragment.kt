package com.h.pixeldroid.fragments.feeds

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.h.pixeldroid.objects.Status
import retrofit2.Call

class PublicTimelineFragment: PostsFeedFragment() {

    inner class SearchFeedDataSource : FeedDataSource(null, null){

        override fun newSource(): SearchFeedDataSource {
            return SearchFeedDataSource()
        }

        private fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return pixelfedAPI.timelinePublic(limit="$requestedLoadSize")
        }
        private fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI.timelinePublic( max_id=key, limit="$requestedLoadSize")
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
    }

    override fun makeContent(): LiveData<PagedList<Status>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = FeedFragment<Status, PostViewHolder>()
            .FeedDataSourceFactory(SearchFeedDataSource())
        return LivePagedListBuilder(factory, config).build()
    }
}