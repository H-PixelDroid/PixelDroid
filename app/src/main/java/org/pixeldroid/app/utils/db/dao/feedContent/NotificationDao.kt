package org.pixeldroid.app.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.api.objects.Notification

@Dao
interface NotificationDao: FeedContentDao<Notification> {

    @Query("DELETE FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=''")
    override suspend fun clearFeedContent(userId: String, instanceUri: String, conversationsId: String)

    @Query("""SELECT * FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=""
            ORDER BY datetime(created_at) DESC""")
    override fun feedContent(userId: String, instanceUri: String, conversationsId: String): PagingSource<Int, Notification>

    @Query("""SELECT * FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri 
            ORDER BY datetime(created_at) DESC LIMIT 1""")
    fun latestNotification(userId: String, instanceUri: String): Notification?

    @Query("DELETE FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id AND :conversationsId=''")
    override suspend fun delete(id: String, userId: String, instanceUri: String, conversationsId: String)
}