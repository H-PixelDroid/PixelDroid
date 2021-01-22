package com.h.pixeldroid.posts.feeds.uncachedFeeds.profile

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedContentRepository
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Status
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileContentRepository @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val accessToken: String,
    private val accountId: String
) : UncachedContentRepository<Status> {
    override fun getStream(): Flow<PagingData<Status>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false),
            pagingSourceFactory = {
                ProfilePagingSource(api, accessToken, accountId)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}