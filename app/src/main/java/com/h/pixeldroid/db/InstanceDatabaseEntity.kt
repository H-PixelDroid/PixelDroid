package com.h.pixeldroid.db

import androidx.room.Entity

@Entity(tableName = "instances", primaryKeys = ["instance", "username"])
data class InstanceDatabaseEntity (
    val instance: String,
    val username: String
)