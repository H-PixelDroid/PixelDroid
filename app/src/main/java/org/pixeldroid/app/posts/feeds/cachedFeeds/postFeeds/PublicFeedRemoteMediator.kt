/*
 * Copyright (C) 2020 The Android Open Source Project
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

package org.pixeldroid.app.posts.feeds.cachedFeeds.postFeeds

import androidx.paging.*
import androidx.room.withTransaction
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.PublicFeedStatusDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import java.lang.NullPointerException
import javax.inject.Inject

/**
 * RemoteMediator for the public feed.
 *
 * A [RemoteMediator] defines a set of callbacks used to incrementally load data from a remote
 * source into a local source wrapped by a [PagingSource], e.g., loading data from network into
 * a local db cache.
 */
@OptIn(ExperimentalPagingApi::class)
class PublicFeedRemoteMediator @Inject constructor(
    private val apiHolder: PixelfedAPIHolder,
    private val db: AppDatabase
) : RemoteMediator<Int, PublicFeedStatusDatabaseEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PublicFeedStatusDatabaseEntity>): MediatorResult {

        val maxId = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.PREPEND -> {
                // No prepend for the moment, might be nice to add later
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> state.lastItemOrNull()?.id
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        try {
            val user = db.userDao().getActiveUser()
                    ?: return MediatorResult.Error(NullPointerException("No active user exists"))
            val api = apiHolder.api ?: apiHolder.setToCurrentUser()

            val apiResponse = api.timelinePublic(
                                                 max_id = maxId,
                                                 limit = state.config.pageSize.toString(),
                                                )

            val dbObjects = apiResponse.map{
                PublicFeedStatusDatabaseEntity(user.user_id, user.instance_uri, it)
            }
            val endOfPaginationReached = apiResponse.isEmpty() || maxId == apiResponse.sortedBy { it.created_at }.last().id

            db.withTransaction {
                // Clear table in the database
                if (loadType == LoadType.REFRESH) {
                    db.publicPostDao().clearFeedContent(user.user_id, user.instance_uri)
                }
                db.publicPostDao().insertAll(dbObjects)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: Exception) {
            return MediatorResult.Error(exception)
        }
    }
}