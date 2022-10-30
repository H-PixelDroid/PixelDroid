package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedContentRepository
import org.pixeldroid.app.utils.api.PixelfedAPI
import kotlinx.coroutines.flow.Flow
import org.pixeldroid.app.utils.api.objects.Collection
import javax.inject.Inject

class CollectionsContentRepository @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val accountId: String,
) : UncachedContentRepository<Collection> {
    override fun getStream(): Flow<PagingData<Collection>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE),
            pagingSourceFactory = {
                CollectionsPagingSource(api, accountId)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}