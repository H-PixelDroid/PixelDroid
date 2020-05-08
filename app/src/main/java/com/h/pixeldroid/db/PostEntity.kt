package com.h.pixeldroid.db

import androidx.room.Embedded
import androidx.room.Entity

data class Account (
    val account_id: String,
    val username: String,
    val acct: String = "",
    val account_url: String = "", //HTTPS URL
    val display_name: String? = null,
    val note: String = "", //HTML
    val avatar: String = "", //URL
    val avatar_static: String = "" //URL
)

@Entity(tableName= "posts", primaryKeys = ["domain", "id"])
data class PostEntity(
    val domain: String,
    val id: String,
    val uri: String = "",
    val created_at: String = "", //ISO 8601 Datetime (maybe can use a date type)
    @Embedded val account: Account,
    val content: String = "", //HTML
    val spoiler_text: String = "",
    val reblogs_count: Int = 0,
    val favourites_count: Int = 0,
    val replies_count: Int = 0,
    val url: String? = null, //URL
    val in_reply_to_id: String? = null,
    val in_reply_to_account: String? = null,
    val text: String? = null,
    //Authorized user attributes
    val favourited: Boolean = false,
    val reblogged: Boolean = false,
    val muted: Boolean = false,
    val bookmarked: Boolean = false,
    val pinned: Boolean = false
)