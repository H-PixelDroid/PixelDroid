package com.h.pixeldroid.posts.feeds.uncachedFeeds.accountLists

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedContentRepository
import com.h.pixeldroid.utils.api.objects.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class FollowersContentRepository @ExperimentalPagingApi
@Inject constructor(
    private val api: PixelfedAPI,
    private val accessToken: String,
    private val accountId: String,
    private val following: Boolean,
): UncachedContentRepository<Account> {
    override fun getStream(): Flow<PagingData<Account>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = NETWORK_PAGE_SIZE,
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false),
            pagingSourceFactory = {
                FollowersPagingSource(api, accessToken, accountId, following)
            }
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}