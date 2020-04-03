package com.h.pixeldroid.objects

abstract class FeedContent {
    abstract val id: String

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}