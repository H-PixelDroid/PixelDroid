package com.h.pixeldroid.objects

import java.util.Date

/*
Represents a notification of an event relevant to the user.
https://docs.joinmastodon.org/entities/notification/
 */
data class Notification(
    //Required attributes
    override val id: String,
    val type: NotificationType,
    val created_at: Date, //ISO 8601 Datetime
    val account: Account,
    //Optional attributes
    val status: Status? = null
): FeedContent() {
    enum class NotificationType {
        follow, mention, reblog, favourite, poll
    }
}