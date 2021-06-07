package org.pixeldroid.app.posts.feeds.uncachedFeeds.hashtags

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedContentRepository
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Results
import kotlinx.coroutines.flow.Flow
import org.pixeldroid.app.utils.api.objects.Status
import javax.inject.Inject

/**
 * Repository class for viewing hashtags
 */
class HashTagContentRepository @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val hashtag: String,
): UncachedContentRepository<Status> {
    override fun getStream(): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false),
            pagingSourceFactory = {
                HashTagPagingSource(api, hashtag)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}