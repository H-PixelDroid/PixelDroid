package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPost(post: PostDatabaseEntity)

    @Query("SELECT COUNT(*) FROM posts")
    fun numberOfPosts(): Int

    @Query("SELECT * FROM posts WHERE user_id=:user AND instance_uri=:instanceUri")
    fun getAll(user: String, instanceUri: String): List<PostDatabaseEntity>

    @Query("SELECT COUNT(*) FROM posts WHERE uri=:uri")
    fun count(uri: String): Int

    @Query(
        """DELETE FROM posts WHERE uri IN (
                        SELECT uri FROM posts ORDER BY date ASC LIMIT :nPosts
                    )"""
    )
    fun removeOlderPosts(nPosts: Int)
}