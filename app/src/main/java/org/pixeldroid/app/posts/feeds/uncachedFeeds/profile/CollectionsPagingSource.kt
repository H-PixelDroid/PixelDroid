package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Collection

class CollectionsPagingSource(
    private val api: PixelfedAPI,
    private val accountId: String,
) : PagingSource<String, Collection>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Collection> {
        return try {
            val posts  = api.accountCollections(accountId)

            LoadResult.Page(
                data = posts,
                prevKey = null,
                //TODO pagination. For now, don't paginate
                nextKey = null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Collection>): String? = null
}