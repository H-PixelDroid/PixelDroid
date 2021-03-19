package com.h.pixeldroid.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

class ProfilePagingSource(
    private val api: PixelfedAPI,
    private val accessToken: String,
    private val accountId: String
) : PagingSource<String, Status>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        val position = params.key
        return try {
            val posts = api.accountPosts("Bearer $accessToken",
                    account_id = accountId,
                    max_id = position,
                    limit = params.loadSize
            )

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = posts.lastOrNull()?.id
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? = null
}