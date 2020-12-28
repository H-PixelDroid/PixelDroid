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

package com.h.pixeldroid.posts.feeds.cachedFeeds.postFeeds

import androidx.paging.*
import androidx.room.withTransaction
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.db.entities.PublicFeedStatusDatabaseEntity
import com.h.pixeldroid.utils.di.PixelfedAPIHolder
import retrofit2.HttpException
import java.io.IOException
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

        val (max_id, min_id) = when (loadType) {
            LoadType.REFRESH -> {
                Pair<String?, String?>(null, null)
            }
            LoadType.PREPEND -> {
                //No prepend for the moment, might be nice to add later
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
                Pair<String?, String?>(state.lastItemOrNull()?.id, null)
            }

        }

        try {
            val user = db.userDao().getActiveUser()!!
            val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

            val apiResponse = api.timelinePublic(
                                                 max_id = max_id,
                                                 min_id = min_id,
                                                 limit = state.config.pageSize.toString(),
                                                )

            val dbObjects = apiResponse.map{
                PublicFeedStatusDatabaseEntity(user.user_id, user.instance_uri, it)
            }
            val endOfPaginationReached = apiResponse.isEmpty()

            db.withTransaction {
                // clear table in the database
                if (loadType == LoadType.REFRESH) {
                    db.publicPostDao().clearFeedContent()
                }
                db.publicPostDao().insertAll(dbObjects)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }
}