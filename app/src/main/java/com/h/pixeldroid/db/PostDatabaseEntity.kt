package com.h.pixeldroid.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts"
)
data class PostDatabaseEntity (
    @PrimaryKey var uri: String,
    var account_profile_picture: String,
    var account_name: String,
    var media_urls: List<String>,
    var favourite_count: Int,
    var reply_count: Int,
    var share_count: Int,
    var description: String,
    var date: String,
    var store_time: String,
    var likes: Int,
    var shares: Int
)