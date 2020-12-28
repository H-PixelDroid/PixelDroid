package com.h.pixeldroid.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.h.pixeldroid.utils.api.objects.Notification

@Dao
interface NotificationDao: FeedContentDao<Notification> {

    @Query("DELETE FROM notifications")
    override suspend fun clearFeedContent()

    @Query("""SELECT * FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri 
            ORDER BY CAST(created_at AS FLOAT) DESC""")
    override fun feedContent(userId: String, instanceUri: String): PagingSource<Int, Notification>

    @Query("DELETE FROM notifications WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id")
    override suspend fun delete(id: String, userId: String, instanceUri: String)
}