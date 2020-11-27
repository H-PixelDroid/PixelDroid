package com.h.pixeldroid.fragments.feeds.uncachedFeeds.search

import androidx.paging.PagingSource
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.FeedContent
import com.h.pixeldroid.objects.Results
import retrofit2.HttpException
import java.io.IOException

/**
 * Provides the PagingSource for search feeds. Is used in [SearchContentRepository]
 */
class SearchPagingSource<T: FeedContent>(
    private val api: PixelfedAPI,
    private val query: String,
    private val type: Results.SearchType,
    private val accessToken: String,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val position = params.key
        return try {
            val response = api.search(authorization = "Bearer $accessToken",
                offset = position?.toString(),
                q = query,
                type = type,
                limit = params.loadSize.toString())


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
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}