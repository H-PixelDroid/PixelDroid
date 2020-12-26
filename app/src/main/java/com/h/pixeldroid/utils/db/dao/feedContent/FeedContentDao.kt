package com.h.pixeldroid.utils.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.h.pixeldroid.utils.api.objects.FeedContentDatabase

interface FeedContentDao<T: FeedContentDatabase>{

    fun feedContent(userId: String, instanceUri: String): PagingSource<Int, T>

    suspend fun clearFeedContent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feedContent: List<T>)

    suspend fun delete(id: String, userId: String, instanceUri: String)

}