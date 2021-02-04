package com.h.pixeldroid.utils.api.objects

data class Instance (
    val description: String?,
    val email: String?,
    val max_toot_chars: String? = DEFAULT_MAX_TOOT_CHARS.toString(),
    val registrations: Boolean?,
    val thumbnail: String?,
    val title: String?,
    val uri: String?,
    val version: String?
) {
    companion object {
        // Default max number of chars for Mastodon: used when their is no other value supplied by
        // either NodeInfo or the instance endpoint
        const val DEFAULT_MAX_TOOT_CHARS = 500
    }
}