package com.h.pixeldroid.utils.api.objects

import java.io.Serializable

data class DiscoverPosts(
    //Required attributes
    val posts: List<DiscoverPost>
) : Serializable
