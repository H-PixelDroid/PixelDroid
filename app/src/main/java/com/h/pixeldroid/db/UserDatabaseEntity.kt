package com.h.pixeldroid.db

import androidx.room.Entity

@Entity(tableName = "users", primaryKeys = arrayOf("user_id", "instance"))
data class UserDatabaseEntity (
    var user_id: String,
    var instance: String,
    var username: String
)