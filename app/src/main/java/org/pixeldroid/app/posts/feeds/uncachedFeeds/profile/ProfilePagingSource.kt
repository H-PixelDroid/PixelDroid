package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status

class ProfilePagingSource(
    private val api: PixelfedAPI,
    private val accountId: String,
    private val bookmarks: Boolean,
    private val collectionId: String?,
) : PagingSource<String, Status>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Status> {
        val position = params.key
        return try {
            val posts =
                if(collectionId != null){
                    api.collectionItems(
                        collectionId,
                        page = position
                    )
                }
                else if(bookmarks) {
                    api.bookmarks(
                        limit = params.loadSize,
                        max_id = position
                    )
                } else {
                    api.accountPosts(
                        account_id = accountId,
                        max_id = position,
                        limit = params.loadSize
                    )
                }

            val nextKey = posts.lastOrNull()?.id

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if(collectionId != null ) {
                    if(posts.isEmpty()) null else (params.key?.toIntOrNull()?.plus(1))?.toString()
                } else if(nextKey == position) null else nextKey
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Status>): String? = null
}