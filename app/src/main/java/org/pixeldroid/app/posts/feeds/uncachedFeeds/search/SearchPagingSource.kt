package org.pixeldroid.app.posts.feeds.uncachedFeeds.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Results

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

            val nextKey = if (repos.isEmpty()) null else (position ?: 0) + repos.size

            LoadResult.Page(
                data = repos,
                prevKey = null,
                nextKey = if(nextKey == position) null else nextKey
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    /**
     * FIXME if implemented with [PagingState.anchorPosition], this breaks refreshes? How is this
     * supposed to work?
     */
    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
}