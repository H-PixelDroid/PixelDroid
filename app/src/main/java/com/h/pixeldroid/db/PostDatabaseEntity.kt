package com.h.pixeldroid.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "posts",
    primaryKeys = ["uri", "user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = UserDatabaseEntity::class,
        parentColumns = arrayOf("user_id", "instance_uri"),
        childColumns = arrayOf("user_id", "instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["user_id"])]
)
data class PostDatabaseEntity (
    var uri: String,
    var user_id: String,
    var instance_uri: String,
    var account_profile_picture: String,
    var account_name: String,
    var media_urls: List<String>,
    var favourite_count: Int,
    var reply_count: Int,
    var share_count: Int,
    var description: String,
    var date: Date,
    var likes: Int,
    var shares: Int
)