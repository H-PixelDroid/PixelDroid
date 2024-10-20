/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pixeldroid.app.posts.feeds.cachedFeeds

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.RemoteMediator
import kotlinx.coroutines.flow.Flow
import org.pixeldroid.app.utils.api.objects.FeedContentDatabase
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao

/**
 * Repository class that works with local and remote data sources.
 */
class FeedContentRepository<T: FeedContentDatabase> @ExperimentalPagingApi constructor(
    private val db: AppDatabase,
    private val dao: FeedContentDao<T>,
    private val mediator: RemoteMediator<Int, T>,
    private val conversationsId: String = "",
) {

    /**
     * [FeedContentDatabase], exposed as a stream of data that will emit
     * every time we get more data from the network.
     */
    @ExperimentalPagingApi
    fun stream(): Flow<PagingData<T>> {
        val user = db.userDao().getActiveUser()!!

        val pagingSourceFactory = {
            dao.feedContent(user.user_id, user.instance_uri, conversationsId)
        }

        return Pager(
                config = PagingConfig(initialLoadSize = NETWORK_PAGE_SIZE,
                    pageSize = NETWORK_PAGE_SIZE,
                    prefetchDistance = 20
                ),
                remoteMediator = mediator,
                pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 20
    }
}