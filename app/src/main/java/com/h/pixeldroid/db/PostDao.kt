package com.h.pixeldroid.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import java.util.Date

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY date DESC")
    fun getAllByDate(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE uid = :postId")
    fun getById(postId: Int): LiveData<PostEntity>

    @Query("SELECT * FROM posts WHERE date IN (SELECT min(date) FROM posts) ")
    fun getOldestPost(): LiveData<PostEntity>

    @Query("SELECT count(*) FROM posts")
    fun getPostsCount(): LiveData<Int?>

    @Query("UPDATE posts SET date = :date WHERE uid = :postId")
    fun addDateToPost(postId: Int, date: Date)

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("DELETE FROM posts WHERE date IN (SELECT min(date) FROM posts) ")
    fun deleteOldestPost()

    @Insert(onConflict = REPLACE)
    fun insertAll(posts: ArrayList<PostEntity>)

    @Delete
    fun delete(post: PostEntity)
}
