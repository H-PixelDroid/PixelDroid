package org.pixeldroid.app.utils.api.objects

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import java.io.Serializable
import java.time.OffsetDateTime

/*
Represents a notification of an event relevant to the user.
https://docs.joinmastodon.org/entities/notification/
 */
@Entity(
    tableName = "notifications",
    primaryKeys = ["id", "user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = UserDatabaseEntity::class,
        parentColumns = arrayOf("user_id", "instance_uri"),
        childColumns = arrayOf("user_id", "instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["user_id", "instance_uri"])]
)
data class Notification(
    //Required attributes
    override val id: String,
    val type: NotificationType?,
    val created_at: OffsetDateTime?, //ISO 8601 Datetime
    val account: Account?,
    //Optional attributes
    val status: Status? = null,

    //Database values (not from API)
    //TODO do we find this approach acceptable? Preferable to a semi-duplicate NotificationDataBaseEntity?
    override var user_id: String,
    override var instance_uri: String,
    ): FeedContent, FeedContentDatabase {
    enum class NotificationType: Serializable {
        follow, mention, reblog, favourite, poll, comment
    }
}