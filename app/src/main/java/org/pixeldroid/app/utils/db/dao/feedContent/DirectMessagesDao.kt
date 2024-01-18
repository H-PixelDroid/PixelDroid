package org.pixeldroid.app.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.db.dao.feedContent.FeedContentDao

@Dao
interface DirectMessagesDao: FeedContentDao<Conversation> {

    @Query("DELETE FROM direct_messages WHERE user_id=:userId AND instance_uri=:instanceUri")
    override suspend fun clearFeedContent(userId: String, instanceUri: String)

    // TODO: might have to order by date or some other value
    @Query("""SELECT * FROM direct_messages WHERE user_id=:userId AND instance_uri=:instanceUri """)
    override fun feedContent(userId: String, instanceUri: String): PagingSource<Int, Conversation>
}