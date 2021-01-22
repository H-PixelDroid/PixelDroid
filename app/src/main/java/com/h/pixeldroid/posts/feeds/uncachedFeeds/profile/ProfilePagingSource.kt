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
            val response = api.accountPosts("Bearer $accessToken", account_id = accountId)

            val posts = if(response.isSuccessful){
                response.body().orEmpty()
            } else {
                throw HttpException(response)
            }

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if(posts.isEmpty()) null else (position ?: 0) + posts.size
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}