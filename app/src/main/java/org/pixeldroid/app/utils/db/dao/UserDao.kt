package org.pixeldroid.app.utils.db.dao

import androidx.room.*
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

@Dao
interface UserDao {
    /**
     * Insert a user, if it already exists return -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUser(user: UserDatabaseEntity): Long

    @Transaction
    fun insertOrUpdate(user: UserDatabaseEntity) {
        if (insertUser(user) == -1L) {
            updateUser(user)
        }
    }

    @Update
    fun updateUser(user: UserDatabaseEntity)

    @Query("UPDATE users SET accessToken = :accessToken, refreshToken = :refreshToken WHERE user_id = :id and instance_uri = :instance_uri")
    fun updateAccessToken(accessToken: String, refreshToken: String, id: String, instance_uri: String)

    @Query("SELECT * FROM users")
    fun getAll(): List<UserDatabaseEntity>

    @Query("SELECT * FROM users WHERE isActive=1 LIMIT 1")
    fun getActiveUser(): UserDatabaseEntity?

    @Query("UPDATE users SET isActive=0")
    fun deActivateActiveUsers()

    //TODO also check instance_uri
    @Query("UPDATE users SET isActive=1 WHERE user_id=:id")
    fun activateUser(id: String)

    @Query("DELETE FROM users WHERE isActive=1")
    fun deleteActiveUsers()

    //TODO also check instance_uri
    @Query("SELECT * FROM users WHERE user_id=:id LIMIT 1")
    fun getUserWithId(id: String): UserDatabaseEntity
}