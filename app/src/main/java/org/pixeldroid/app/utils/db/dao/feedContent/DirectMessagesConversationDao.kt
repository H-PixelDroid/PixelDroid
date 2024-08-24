package org.pixeldroid.app.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.db.entities.DirectMessageDatabaseEntity

@Dao
interface DirectMessagesConversationDao: FeedContentDao<DirectMessageDatabaseEntity> {

    @Query("DELETE FROM directMessagesThreads WHERE user_id=:userId AND instance_uri=:instanceUri AND conversationsId=:conversationsId")
    override suspend fun clearFeedContent(userId: String, instanceUri: String, conversationsId: String)

    //TODO think about ordering
    @Query("SELECT * FROM directMessagesThreads WHERE user_id=:userId AND instance_uri=:instanceUri AND conversationsId=:conversationsId ORDER BY datetime(created_at) DESC")
    override fun feedContent(userId: String, instanceUri: String, conversationsId: String): PagingSource<Int, DirectMessageDatabaseEntity>

    @Query("DELETE FROM directMessagesThreads WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id AND conversationsId=:conversationsId")
    override suspend fun delete(id: String, userId: String, instanceUri: String, conversationsId: String)

    @Query("SELECT id FROM directMessagesThreads WHERE user_id=:userId AND instance_uri=:instanceUri AND conversationsId=:conversationsId ORDER BY datetime(created_at) ASC LIMIT 1")
    suspend fun lastMessageId(userId: String, instanceUri: String, conversationsId: String): String?
}