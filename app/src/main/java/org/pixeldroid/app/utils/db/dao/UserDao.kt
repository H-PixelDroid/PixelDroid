package org.pixeldroid.app.utils.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

@Dao
interface UserDao {
    /**
     * Insert a user, if it already exists return -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserDatabaseEntity): Long

    @Transaction
    suspend fun insertOrUpdate(user: UserDatabaseEntity) {
        if (insertUser(user) == -1L) {
            updateUser(user)
        }
    }

    @Update
    suspend fun updateUser(user: UserDatabaseEntity)

    @Query("UPDATE users SET username = :username, display_name = :displayName, avatar_static = :avatarStatic WHERE user_id = :id and instance_uri = :instanceUri")
    suspend fun updateUserAccountDetails(username: String, displayName: String, avatarStatic: String, id: String, instanceUri: String)


    @Query("UPDATE users SET accessToken = :accessToken, refreshToken = :refreshToken WHERE user_id = :id and instance_uri = :instanceUri")
    fun updateAccessToken(accessToken: String, refreshToken: String, id: String, instanceUri: String)

    @Query("SELECT * FROM users")
    fun getAll(): List<UserDatabaseEntity>

    @Query("SELECT * FROM users")
    fun getAllFlow(): Flow<List<UserDatabaseEntity>>

    @Query("SELECT * FROM users WHERE isActive=1")
    fun getActiveUser(): UserDatabaseEntity?

    @Query("UPDATE users SET isActive=0")
    fun deActivateActiveUsers()

    @Query("UPDATE users SET isActive=1 WHERE user_id=:id AND instance_uri=:instanceUri")
    fun activateUser(id: String, instanceUri: String)

    @Query("DELETE FROM users WHERE isActive=1")
    fun deleteActiveUsers()

    @Query("SELECT * FROM users WHERE user_id=:id AND instance_uri=:instanceUri")
    fun getUserWithId(id: String, instanceUri: String): UserDatabaseEntity
}