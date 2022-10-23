package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedContentRepository
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileContentRepository @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val accountId: String,
    private val bookmarks: Boolean
) : UncachedContentRepository<Status> {
    override fun getStream(): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE),
            pagingSourceFactory = {
                ProfilePagingSource(api, accountId, bookmarks)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}