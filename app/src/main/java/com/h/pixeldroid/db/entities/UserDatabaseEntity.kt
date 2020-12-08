package com.h.pixeldroid.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "users",
    primaryKeys = ["user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = InstanceDatabaseEntity::class,
        parentColumns = arrayOf("uri"),
        childColumns = arrayOf("instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["instance_uri"])]
)
data class UserDatabaseEntity(
        var user_id: String,
        var instance_uri: String,
        var username: String,
        var display_name: String,
        var avatar_static: String,
        var isActive: Boolean,
        var accessToken: String,
        val refreshToken: String?,
        val clientId: String,
        val clientSecret: String
)