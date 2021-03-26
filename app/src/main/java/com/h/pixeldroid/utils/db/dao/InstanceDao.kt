package com.h.pixeldroid.utils.db.dao

import androidx.room.*
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances")
    fun getAll(): List<InstanceDatabaseEntity>

    /**
     * Insert an instance, if it already exists return -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertInstance(instance: InstanceDatabaseEntity): Long

    @Update
    fun updateInstance(instance: InstanceDatabaseEntity)

    @Transaction
    fun insertOrUpdate(instance: InstanceDatabaseEntity) {
        if (insertInstance(instance) == -1L) {
            updateInstance(instance)
        }
    }
}