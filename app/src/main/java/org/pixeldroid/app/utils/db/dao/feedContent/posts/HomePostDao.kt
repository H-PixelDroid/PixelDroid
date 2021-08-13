package org.pixeldroid.app.utils.db.dao.feedContent.posts

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao
import org.pixeldroid.app.utils.db.entities.HomeStatusDatabaseEntity

@Dao
interface HomePostDao: FeedContentDao<HomeStatusDatabaseEntity> {
    @Query("""SELECT * FROM homePosts WHERE user_id=:userId AND instance_uri=:instanceUri 
            ORDER BY CAST(created_at AS FLOAT)""")
    override fun feedContent(userId: String, instanceUri: String): PagingSource<Int, HomeStatusDatabaseEntity>

    @Query("DELETE FROM homePosts")
    override suspend fun clearFeedContent()

    @Query("DELETE FROM homePosts WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id")
    override suspend fun delete(id: String, userId: String, instanceUri: String)

}