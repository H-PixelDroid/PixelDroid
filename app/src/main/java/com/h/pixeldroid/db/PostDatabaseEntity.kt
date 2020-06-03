package com.h.pixeldroid.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts"
)
data class PostDatabaseEntity (
    @PrimaryKey var uri: String,
    val account_profile_picture: String,
    val account_name: String,
    val media_urls: List<String>,
    val favourite_count: Int,
    val reply_count: Int,
    val share_count: Int,
    val description: String,
    val date: String,
    val store_time: String
)