package org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Results
import org.pixeldroid.app.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

/**
 * Provides the PagingSource for hashtag feeds. Is used in [HashTagContentRepository]
 */
class HashTagPagingSource(
    private val api: PixelfedAPI,
    private val query: String,
) : PagingSource<String, Status>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        val position = params.key
        return try {
            val response = api.hashtag(
                hashtag = query,
                limit = params.loadSize,
                max_id = position,
            )

            LoadResult.Page(
                data = response,
                prevKey = null,
                nextKey = response.lastOrNull()?.id
            )
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        }
    }

    /**
     * FIXME if implemented with [PagingState.anchorPosition], this breaks refreshes? How is this
     * supposed to work?
     */
    override fun getRefreshKey(state: PagingState<String, Status>): String? = null
}