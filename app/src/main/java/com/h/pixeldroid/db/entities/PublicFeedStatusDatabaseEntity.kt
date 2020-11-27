package com.h.pixeldroid.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.h.pixeldroid.objects.*
import java.util.*

@Entity(
    tableName = "publicPosts",
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
class PublicFeedStatusDatabaseEntity(
    override var user_id: String,
    override var instance_uri: String,
    status: Status
): Status(
    status.id,
    status.uri,
    status.created_at,
    status.account,
    status.content,
    status.visibility,
    status.sensitive,
    status.spoiler_text,
    status.media_attachments,
    status.application,
    status.mentions,
    status.tags,
    status.emojis,
    status.reblogs_count,
    status.favourites_count,
    status.replies_count,
    status.url,
    status.in_reply_to_id,
    status.in_reply_to_account,
    status.reblog,
    status.poll,
    status.card,
    status.language,
    status.text,
    status.favourited,
    status.reblogged,
    status.muted,
    status.bookmarked,
    status.pinned
), FeedContentDatabase {
    //Constructor to make Room happy. This sucks, and I know it.
    constructor(id: String,
                uri: String? = "",
                created_at: Date? = Date(0),
                account: Account?,
                content: String? = "",
                visibility: Visibility? = Visibility.public,
                sensitive: Boolean? = false,
                spoiler_text: String? = "",
                media_attachments: List<Attachment>? = null,
                application: Application? = null,

                mentions: List<Mention>? = null,
                tags: List<Tag>? = null,
                emojis: List<Emoji>? = null,

                reblogs_count: Int? = 0,
                favourites_count: Int? = 0,
                replies_count: Int? = 0,

                url: String? = null,
                in_reply_to_id: String? = null,
                in_reply_to_account: String? = null,
                reblog: Status? = null,
                poll: Poll? = null,
                card: Card? = null,
                language: String? = null,
                text: String? = null,

                favourited: Boolean? = false,
                reblogged: Boolean? = false,
                muted: Boolean? = false,
                bookmarked: Boolean? = false,
                pinned: Boolean? = false,
                user_id: String,
                instance_uri: String): this(user_id, instance_uri, Status(id, uri, created_at, account, content, visibility, sensitive, spoiler_text, media_attachments, application, mentions, tags, emojis, reblogs_count, favourites_count, replies_count, url, in_reply_to_id, in_reply_to_account, reblog, poll, card, language, text, favourited, reblogged, muted, bookmarked, pinned)
    )

}