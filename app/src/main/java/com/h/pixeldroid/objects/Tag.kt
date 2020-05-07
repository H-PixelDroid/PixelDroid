package com.h.pixeldroid.objects

import java.io.Serializable

data class Tag(
    //Base attributes
    val name: String,
    val url: String,
    //Optional attributes
    val history: List<History>? = emptyList()) : Serializable, FeedContent() {
    //needed to be a FeedContent, this inheritance is a bit fickle. Do not use.
    override val id: String
        get() = "tag"

}

