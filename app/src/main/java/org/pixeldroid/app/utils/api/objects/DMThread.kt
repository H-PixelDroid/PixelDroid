package org.pixeldroid.app.utils.api.objects

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import java.io.Serializable
import java.time.Instant


/*
Represents a conversation.
https://docs.joinmastodon.org/entities/Conversation/
 */
data class DMThread(
    override val id: String,
    val name: String?,
    val username: String?,
    val avatar: String?,
    val url: String?,
    val muted: Boolean?,
    val isLocal: Boolean?,
    val domain: String?,
    val created_at: Instant?, //ISO 8601 Datetime
    val updated_at: Instant?,
    val timeAgo: String?,
    val lastMessage: String?,
    val messages: List<Message>,
): FeedContent, Serializable