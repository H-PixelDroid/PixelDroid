package com.h.pixeldroid.db.dao.feedContent

import androidx.paging.PagingSource
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.h.pixeldroid.objects.FeedContentDatabase

interface FeedContentDao<T: FeedContentDatabase>{

    fun feedContent(userId: String, instanceUri: String): PagingSource<Int, T>

    suspend fun clearFeedContent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feedContent: List<T>)

}