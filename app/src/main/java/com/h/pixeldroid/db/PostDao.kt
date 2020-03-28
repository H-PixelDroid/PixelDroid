package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import java.util.Date

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY date DESC")
    fun getAllByDate(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE uid = :postId")
    fun getById(postId: Long): PostEntity

    @Query("SELECT * FROM posts WHERE date IN (SELECT min(date) FROM posts) ")
    fun getOldestPost(): PostEntity

    @Query("SELECT count(*) FROM posts")
    fun getPostCount(): Int

    @Query("UPDATE posts SET date = :date WHERE uid = :postId")
    fun addDateToPost(postId: Long, date: Date)

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("DELETE FROM posts WHERE date IN (SELECT min(date) FROM posts) ")
    fun deleteOldestPost()

    @Insert(onConflict = REPLACE)
    fun insertPost(post: PostEntity): Long

    @Delete
    fun delete(post: PostEntity)
}
