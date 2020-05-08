package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    fun getAll(): List<PostEntity>

    @Insert(onConflict = REPLACE)
    fun insertAll(vararg posts: PostEntity)

    @Delete
    fun delete(post: PostEntity)
}
