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

package org.pixeldroid.app.directmessages

import androidx.paging.*
import androidx.room.withTransaction
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.db.entities.DirectMessageDatabaseEntity
import java.lang.Exception
import java.lang.NullPointerException
import javax.inject.Inject

/**
 * RemoteMediator for a Direct Messages conversation.
 *
 * A [RemoteMediator] defines a set of callbacks used to incrementally load data from a remote
 * source into a local source wrapped by a [PagingSource], e.g., loading data from network into
 * a local db cache.
 */
@OptIn(ExperimentalPagingApi::class)
class ConversationRemoteMediator @Inject constructor(
    private val apiHolder: PixelfedAPIHolder,
    private val db: AppDatabase,
    private val pid: String
) : RemoteMediator<Int, DirectMessageDatabaseEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, DirectMessageDatabaseEntity>): MediatorResult {
        try {
            val user = db.userDao().getActiveUser()
                ?: return MediatorResult.Error(NullPointerException("No active user exists"))

            val nextPage = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> {
                    // No prepend for the moment, might be nice to add later
                    state.lastItemOrNull()?.id?.toIntOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)
            }

            val api = apiHolder.api ?: apiHolder.setToCurrentUser()
            val apiResponse =
                    api.directMessagesConversation(
                        pid = pid,
                        max_id = nextPage?.toString(),
                    )
            //TODO prepend

            val messages = apiResponse.messages.map{
                DirectMessageDatabaseEntity(
                    it,
                    apiResponse.id,
                    user
                )
            }

            val endOfPaginationReached = messages.isEmpty()

            db.withTransaction {
                // Clear table in the database
                if (loadType == LoadType.REFRESH) {
                    db.directMessagesConversationDao().clearFeedContent(user.user_id, user.instance_uri, apiResponse.id)
                }
                db.directMessagesConversationDao().insertAll(messages)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        }  catch (exception: Exception){
            return MediatorResult.Error(exception)
        }
    }
}