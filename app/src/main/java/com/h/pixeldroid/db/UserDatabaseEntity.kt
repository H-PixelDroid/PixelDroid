package com.h.pixeldroid.db

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "users",
    primaryKeys = ["user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = InstanceDatabaseEntity::class,
        parentColumns = arrayOf("uri"),
        childColumns = arrayOf("instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserDatabaseEntity (
    var user_id: String,
    var instance_uri: String,
    var username: String,
    var display_name: String,
    var avatar_static: String,
    var isActive: Boolean,
    var accessToken: String,
    var latestNotificationId : Int
)