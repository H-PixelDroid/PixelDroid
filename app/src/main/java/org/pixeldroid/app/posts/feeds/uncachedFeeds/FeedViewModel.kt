package org.pixeldroid.app.posts.feeds.uncachedFeeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import org.pixeldroid.app.utils.api.objects.FeedContent
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for the uncached feeds.
 * The ViewModel works with the different [UncachedContentRepository]s to get the data.
 */
class FeedViewModel<T: FeedContent>(repository: UncachedContentRepository<T>) : ViewModel() {
    val flow: Flow<PagingData<T>> = repository.getStream().cachedIn(viewModelScope)
}

/**
 * Common interface for the different uncached feeds
 */
interface UncachedContentRepository<T: FeedContent>{
    fun getStream(): Flow<PagingData<T>>
}