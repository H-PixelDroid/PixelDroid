package com.h.pixeldroid.utils.db.dao.feedContent.posts

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.h.pixeldroid.utils.db.dao.feedContent.FeedContentDao
import com.h.pixeldroid.utils.db.entities.PublicFeedStatusDatabaseEntity

@Dao
interface PublicPostDao: FeedContentDao<PublicFeedStatusDatabaseEntity> {
    @Query("""SELECT * FROM publicPosts WHERE user_id=:userId AND instance_uri=:instanceUri 
            ORDER BY CAST(created_at AS FLOAT)""")
    override fun feedContent(userId: String, instanceUri: String): PagingSource<Int, PublicFeedStatusDatabaseEntity>

    @Query("DELETE FROM publicPosts")
    override suspend fun clearFeedContent()

    @Query("DELETE FROM publicPosts WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id")
    override suspend fun delete(id: String, userId: String, instanceUri: String)

}