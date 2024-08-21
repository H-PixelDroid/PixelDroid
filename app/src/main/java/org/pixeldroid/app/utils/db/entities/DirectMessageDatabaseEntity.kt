package org.pixeldroid.app.utils.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.FeedContent
import org.pixeldroid.app.utils.api.objects.FeedContentDatabase
import org.pixeldroid.app.utils.api.objects.Message
import java.io.Serializable
import java.time.Instant

@Entity(
    tableName = "directMessagesThreads",
    primaryKeys = ["id", "conversationsId", "user_id", "instance_uri"],
    foreignKeys = [ForeignKey(
        entity = UserDatabaseEntity::class,
        parentColumns = arrayOf("user_id", "instance_uri"),
        childColumns = arrayOf("user_id", "instance_uri"),
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["user_id", "instance_uri", "conversationsId"])]
)
data class DirectMessageDatabaseEntity(
    override val id: String,
    val name: String?,
    val hidden: Boolean?,
    val isAuthor: Boolean?,
    val type: String?, //TODO enum?
    val text: String?,
    val media: String?, //TODO,
    val carousel: List<Attachment>?,
    val created_at: Instant?, //ISO 8601 Datetime
    val timeAgo: String?,
    val reportId: String?,
    //val meta: String?, //TODO

    // Database values (not from API)
    val conversationsId: String,
    override var user_id: String,
    override var instance_uri: String,
): FeedContent, FeedContentDatabase, Serializable {
    constructor(message: Message, conversationsId: String, user: UserDatabaseEntity) : this(
        message.id,
        message.name,
        message.hidden,
        message.isAuthor,
        message.type,
        message.text,
        message.media,
        message.carousel,
        message.created_at,
        message.timeAgo,
        message.reportId,
        //message.meta,

        conversationsId,
        user.user_id,
        user.instance_uri
    )
}