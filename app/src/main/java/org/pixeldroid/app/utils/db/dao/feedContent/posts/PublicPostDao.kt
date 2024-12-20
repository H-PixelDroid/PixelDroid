package org.pixeldroid.app.utils.db.dao.feedContent.posts

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao
import org.pixeldroid.app.utils.db.entities.PublicFeedStatusDatabaseEntity

@Dao
interface PublicPostDao: FeedContentDao<PublicFeedStatusDatabaseEntity> {
    @Query("""SELECT * FROM publicPosts WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=""
            ORDER BY datetime(created_at) DESC""")
    override fun feedContent(userId: String, instanceUri: String, conversationsId: String): PagingSource<Int, PublicFeedStatusDatabaseEntity>

    @Query("DELETE FROM publicPosts WHERE user_id=:userId AND instance_uri=:instanceUri AND :conversationsId=''")
    override suspend fun clearFeedContent(userId: String, instanceUri: String, conversationsId: String)

    @Query("DELETE FROM publicPosts WHERE user_id=:userId AND instance_uri=:instanceUri AND id=:id AND :conversationsId=''")
    override suspend fun delete(id: String, userId: String, instanceUri: String, conversationsId: String)

    @Query("UPDATE homePosts SET bookmarked=:bookmarked WHERE user_id=:id AND instance_uri=:instanceUri AND id=:statusId")
    fun bookmarkStatus(id: String, instanceUri: String, statusId: String, bookmarked: Boolean)

}