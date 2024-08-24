package org.pixeldroid.app.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.api.objects.Conversation

@Dao
interface DirectMessagesDao: FeedContentDao<Conversation> {

    @Query("DELETE FROM directMessages WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=''")
    override suspend fun clearFeedContent(userId: String, instanceUri: String, conversationsId: String)

    //TODO think about ordering
    @Query("""SELECT * FROM directMessages WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=""""")
    override fun feedContent(userId: String, instanceUri: String, conversationsId: String): PagingSource<Int, Conversation>

    @Query("DELETE FROM directMessages WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id AND :conversationsId=''")
    override suspend fun delete(id: String, userId: String, instanceUri: String, conversationsId: String)
}