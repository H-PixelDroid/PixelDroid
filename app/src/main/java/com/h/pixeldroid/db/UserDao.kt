package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserDatabaseEntity)

    @Query("SELECT * FROM users")
    fun getAll(): List<UserDatabaseEntity>

    @Query("SELECT * FROM users WHERE user_id=:id LIMIT 1")
    fun getUserWithId(id: String): UserDatabaseEntity

    @Query("UPDATE users SET username = :username, display_name = :display_name, avatar_static = :avatar_static WHERE user_id=:user_id")
    fun updateUser(user_id: String, username: String, display_name: String, avatar_static: String)
}