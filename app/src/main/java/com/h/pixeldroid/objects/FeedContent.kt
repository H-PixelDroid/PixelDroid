package com.h.pixeldroid.objects

interface FeedContent {
    val id: String?
}

interface FeedContentDatabase {
    val id: String?

    var user_id: String

    var instance_uri: String
}