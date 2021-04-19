package com.h.pixeldroid.posts.feeds.uncachedFeeds.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.FeedContent
import com.h.pixeldroid.utils.api.objects.Results
import retrofit2.HttpException
import java.io.IOException

/**
 * Provides the PagingSource for search feeds. Is used in [SearchContentRepository]
 */
class SearchPagingSource<T: FeedContent>(
    private val api: PixelfedAPI,
    private val query: String,
    private val type: Results.SearchType,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val position = params.key
        return try {
            val response = api.search(
                type = type,
                q = query,
                limit = params.loadSize.toString(),
                offset = position?.toString()
            )


            @Suppress("UNCHECKED_CAST")
            val repos = when(type){
                Results.SearchType.accounts -> response.accounts
                Results.SearchType.hashtags -> response.hashtags
                Results.SearchType.statuses -> response.statuses
            } as List<T>

            LoadResult.Page(
                data = repos,
                prevKey = null,
                nextKey = if (repos.isEmpty()) null else (position ?: 0) + repos.size
            )
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.run {
            state.closestItemToPosition(this)?.id?.toIntOrNull()
        }
}