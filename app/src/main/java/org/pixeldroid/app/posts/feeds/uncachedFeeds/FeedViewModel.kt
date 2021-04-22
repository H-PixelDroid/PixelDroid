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
class FeedViewModel<T: FeedContent>(private val repository: UncachedContentRepository<T>) : ViewModel() {

    private var currentResult: Flow<PagingData<T>>? = null

    fun flow(): Flow<PagingData<T>> {
        val lastResult = currentResult
        if (lastResult != null) {
            return lastResult
        }
        val newResult: Flow<PagingData<T>> = repository.getStream()
            .cachedIn(viewModelScope)
        currentResult = newResult
        return newResult
    }
}

/**
 * Common interface for the different uncached feeds
 */
interface UncachedContentRepository<T: FeedContent>{
    fun getStream(): Flow<PagingData<T>>
}