package com.h.pixeldroid.db

import androidx.room.Entity

@Entity(
    tableName = "posts"
)
data class PostDatabaseEntity (
    var account_profile_picture: String,
    var account_name: String,
    var picture: String,
    var favourite_count: Int,
    var reply_count: Int
)