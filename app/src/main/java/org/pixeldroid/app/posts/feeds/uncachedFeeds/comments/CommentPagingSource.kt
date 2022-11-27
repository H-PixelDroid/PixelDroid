package org.pixeldroid.app.posts.feeds.uncachedFeeds.comments

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status

class CommentPagingSource(
    private val api: PixelfedAPI,
    private val statusId: String
) : PagingSource<String, Status>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        //val position = params.key
        return try {
            val comments = api.statusComments(statusId).descendants

            // TODO use pagination to have many comments also work
            // For now, don't paginate (nextKey and prevKey null)
            LoadResult.Page(
                data = comments,
                prevKey = null,
                nextKey = null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? = null
}