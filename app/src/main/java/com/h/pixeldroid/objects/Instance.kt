package com.h.pixeldroid.objects

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
        const val DEFAULT_MAX_TOOT_CHARS = 500
    }
}