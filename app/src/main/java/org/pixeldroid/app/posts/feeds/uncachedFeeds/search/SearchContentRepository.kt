package org.pixeldroid.app.posts.feeds.uncachedFeeds.search

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedContentRepository
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.Results
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository class to perform searches
 *
 * The type argument [T] and the [Results.SearchType][type] argument should always
 * be in agreement, e.g. if [T] is a [org.pixeldroid.app.utils.api.objects.Account] then
 * [type] should be [Results.SearchType.accounts].
 */
class SearchContentRepository<T: FeedContent> @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val type: Results.SearchType,
    private val query: String,
): UncachedContentRepository<T> {
    override fun getStream(): Flow<PagingData<T>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false),
            pagingSourceFactory = {
                SearchPagingSource<T>(api, query, type)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}