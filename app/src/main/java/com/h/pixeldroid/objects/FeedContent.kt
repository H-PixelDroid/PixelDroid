package com.h.pixeldroid.objects

abstract class FeedContent {
    abstract val id: String?

    override fun hashCode(): Int {
        return id.hashCode()
    }

}