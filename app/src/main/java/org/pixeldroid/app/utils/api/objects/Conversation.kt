package org.pixeldroid.app.utils.api.objects

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import java.io.Serializable

/*
Represents a conversation.
https://docs.joinmastodon.org/entities/Conversation/
 */

@Entity(
    tableName = "direct_messages",
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
data class Conversation(
    //Base attributes
    override val id: String?,
    val unread: Boolean? = true,
    val accounts: List<Account>? = null,
    val last_status: Status? = null,

    //Database values (not from API)
    //TODO do we find this approach acceptable? Preferable to a semi-duplicate NotificationDataBaseEntity?
    override var user_id: String,
    override var instance_uri: String,
    ): FeedContent, FeedContentDatabase, Serializable {
    enum class NotificationType : Serializable {
        follow, follow_request, mention, reblog, favourite, poll, status, comment //comment is Pixelfed-specific?
    }
}