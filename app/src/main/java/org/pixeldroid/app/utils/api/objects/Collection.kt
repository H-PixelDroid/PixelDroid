package org.pixeldroid.app.utils.api.objects

import java.io.Serializable
import java.time.Instant

data class Collection(
    override val id: String, // Id of the profile
    val pid: String, // Account id
    val visibility: Visibility, // Public or private, or draft for your own collections
    val title: String,
    val description: String,
    val thumb: String, // URL to the thumbnail of this collection
    val updated_at: Instant,
    val published_at: Instant,
    val avatar: String, // URL to the avatar of the author of this collection
    val username: String, // Username of author
    val post_count: Int, //Number of posts in collection
): FeedContent, Serializable {
    enum class Visibility: Serializable {
        public, private, draft
    }
}
