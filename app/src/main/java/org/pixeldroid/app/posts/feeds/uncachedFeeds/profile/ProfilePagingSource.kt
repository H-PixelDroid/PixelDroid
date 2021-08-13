package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

class ProfilePagingSource(
    private val api: PixelfedAPI,
    private val accountId: String
) : PagingSource<String, Status>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        val position = params.key
        return try {
            val posts = api.accountPosts(
                account_id = accountId,
                max_id = position,
                limit = params.loadSize
            )

            val nextKey = posts.lastOrNull()?.id

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if(nextKey == position) null else nextKey
            )
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? = null
}