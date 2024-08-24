package org.pixeldroid.app.directmessages

import android.util.Log
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
    private val pid: String,
    private val conversationId: String
) : RemoteMediator<Int, DirectMessageDatabaseEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, DirectMessageDatabaseEntity>): MediatorResult {
        try {
            val user = db.userDao().getActiveUser()
                ?: return MediatorResult.Error(NullPointerException("No active user exists"))

            val nextPage = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> {
                    // No prepend for the moment, might be nice to add later
                    db.directMessagesConversationDao().lastMessageId(user.user_id, user.instance_uri, conversationId)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)
            }

            val api = apiHolder.api ?: apiHolder.setToCurrentUser()
            val apiResponse =
                    api.directMessagesConversation(
                        pid = pid,
                        max_id = nextPage,
                    )
            //TODO prepend

            val messages = apiResponse.messages.map {
                DirectMessageDatabaseEntity(
                    it,
                    conversationId,
                    user
                )
            }

            val endOfPaginationReached = messages.isEmpty()

            db.withTransaction {
                // Clear table in the database
                if (loadType == LoadType.REFRESH) {
                    db.directMessagesConversationDao().clearFeedContent(user.user_id, user.instance_uri, conversationId)
                }
                db.directMessagesConversationDao().insertAll(messages)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        }  catch (exception: Exception){
            return MediatorResult.Error(exception)
        }
    }
}