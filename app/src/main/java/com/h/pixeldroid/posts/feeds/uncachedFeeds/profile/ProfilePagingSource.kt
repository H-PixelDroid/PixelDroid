package com.h.pixeldroid.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

class ProfilePagingSource(
    private val api: PixelfedAPI,
    private val accessToken: String,
    private val accountId: String
) : PagingSource<Int, Status>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Status> {
        val position = params.key
        return try {
            val posts = api.accountPosts("Bearer $accessToken",
                    account_id = accountId,
                    min_id = position?.toString(),
                    limit = params.loadSize
            )

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = posts.lastOrNull()?.id?.toIntOrNull()
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}