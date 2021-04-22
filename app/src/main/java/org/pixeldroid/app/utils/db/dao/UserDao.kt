package org.pixeldroid.app.utils.db.dao

import androidx.room.*
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserDatabaseEntity)

    @Query("UPDATE users SET accessToken = :accessToken, refreshToken = :refreshToken WHERE user_id = :id and instance_uri = :instance_uri")
    fun updateAccessToken(accessToken: String, refreshToken: String, id: String, instance_uri: String)

    @Query("SELECT * FROM users")
    fun getAll(): List<UserDatabaseEntity>

    @Query("SELECT * FROM users WHERE isActive=1 LIMIT 1")
    fun getActiveUser(): UserDatabaseEntity?

    @Query("UPDATE users SET isActive=0")
    fun deActivateActiveUsers()

    @Query("UPDATE users SET isActive=1 WHERE user_id=:id")
    fun activateUser(id: String)

    @Query("DELETE FROM users WHERE isActive=1")
    fun deleteActiveUsers()

    @Query("SELECT * FROM users WHERE user_id=:id LIMIT 1")
    fun getUserWithId(id: String): UserDatabaseEntity
}