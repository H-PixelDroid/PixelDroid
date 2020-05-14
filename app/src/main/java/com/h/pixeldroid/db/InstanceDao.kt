package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances")
    fun getAll(): List<InstanceDatabaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInstance(instance: InstanceDatabaseEntity)
}