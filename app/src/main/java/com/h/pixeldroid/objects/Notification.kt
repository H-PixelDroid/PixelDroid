package com.h.pixeldroid.objects

/*
Represents a notification of an event relevant to the user.
https://docs.joinmastodon.org/entities/notification/
 */
data class Notification (
    //Required attributes
    val id: String,
    val type: NotificationType,
    val created_at: String, //ISO 8601 Datetime
    val account: Account,
    //Optional attributes
    val status: Status? = null
) {
    enum class NotificationType {
        follow, mention, reblog, favourite, poll
    }
}