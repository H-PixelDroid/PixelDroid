package org.pixeldroid.app.utils.api.objects

import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import java.io.Serializable
import java.time.Instant

data class StoryCarousel(
    val self: CarouselUserContainer?,
    val nodes: List<CarouselUserContainer?>?
): Serializable

data class CarouselUser(
    val id: String?,
    val username: String?,
    val username_acct: String?,
    val avatar: String?, // URL to account avatar
    val local: Boolean?, // Is this story from the local instance?
    val is_author: Boolean?, // Is this me? (seems redundant with id)
): Serializable

/**
 * Container with a description of the [user] and a list of stories ([nodes])
 */
data class CarouselUserContainer(
    val user: CarouselUser?,
    val nodes: List<Story?>?,
): Serializable {
    constructor(user: UserDatabaseEntity, nodes: List<Story?>?) : this(
        CarouselUser(user.user_id, user.username, null, user.avatar_static,
            local = true,
            is_author = true
        ), nodes)
}

data class Story(
    val id: String?,
    val pid: String?, // id of author
    val type: String?, //TODO make enum of this? examples: "photo", ???
    val src: String?, // URL to photo of story
    val duration: Int?, //Time in seconds that the Story should be shown
    val seen: Boolean?, //Indication of whether this story has been seen. Set to true using carouselSeen
    val created_at: Instant?, //ISO 8601 Datetime
): Serializable