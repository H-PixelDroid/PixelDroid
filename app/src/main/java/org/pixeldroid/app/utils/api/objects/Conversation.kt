package org.pixeldroid.app.utils.api.objects

import java.io.Serializable

/*
Represents a conversation.
https://docs.joinmastodon.org/entities/Conversation/
 */

data class Conversation(
    //Base attributes
    override val id: String?,
    val unread: Boolean? = true,
    val accounts: List<Account>? = null,
    val last_statuses: Status? = null
) : Serializable, FeedContent
