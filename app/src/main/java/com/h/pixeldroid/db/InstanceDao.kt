package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances")
    fun getAll(): List<Instance>

    @Insert
    fun insertAll(vararg instances: Instance)

    @Delete
    fun delete(instance: Instance)
}