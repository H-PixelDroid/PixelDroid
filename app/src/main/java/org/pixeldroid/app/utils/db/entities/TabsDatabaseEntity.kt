package org.pixeldroid.app.utils.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tabsChecked",
    primaryKeys = ["index", "user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = UserDatabaseEntity::class,
        parentColumns = arrayOf("user_id", "instance_uri"),
        childColumns = arrayOf("user_id", "instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["user_id", "instance_uri"])]
)
data class TabsDatabaseEntity(
    var user_id: String,
    var instance_uri: String,
    var index: Int,
    var tab: String,
    var checked: Boolean = true,
)