package org.pixeldroid.app.utils.api.objects

import java.io.Serializable

data class DiscoverPosts(
    //Required attributes
    val posts: List<Status>
) : Serializable
