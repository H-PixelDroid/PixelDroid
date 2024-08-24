package org.pixeldroid.app.utils.api.objects

import java.io.Serializable
import java.time.Instant

data class Message(
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
): FeedContent, Serializable
