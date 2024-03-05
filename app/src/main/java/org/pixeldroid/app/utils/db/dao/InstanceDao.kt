package org.pixeldroid.app.utils.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances WHERE uri=:instanceUri")
    fun getInstance(instanceUri: String): InstanceDatabaseEntity


    @Query("SELECT * FROM instances WHERE uri=(SELECT users.instance_uri FROM users WHERE isActive=1)")
    fun getActiveInstance(): InstanceDatabaseEntity

    /**
     * Insert an instance, if it already exists return -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstance(instance: InstanceDatabaseEntity): Long

    @Update
    suspend fun updateInstance(instance: InstanceDatabaseEntity)

    @Transaction
    suspend fun insertOrUpdate(instance: InstanceDatabaseEntity) {
        if (insertInstance(instance) == -1L) {
            updateInstance(instance)
        }
    }
}