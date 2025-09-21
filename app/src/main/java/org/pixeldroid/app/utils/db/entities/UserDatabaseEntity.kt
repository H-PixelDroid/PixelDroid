package org.pixeldroid.app.utils.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.io.Serializable

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
): Serializable {
    val fullHandle: String
        get() = "@${username}@${instance_uri.removePrefix("https://")}"

    // We need a long for the drawer account list
    fun stableId(): Long = fnv1a32("$user_id|$instance_uri")
}

fun fnv1a32(input: String): Long {
    var hash = 0x811c9dc5L
    val prime = 0x01000193L
    for (c in input) {
        hash = hash xor c.code.toLong()
        hash *= prime
    }
    return hash
}