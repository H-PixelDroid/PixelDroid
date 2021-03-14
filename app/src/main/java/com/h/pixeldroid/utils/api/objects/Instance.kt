package com.h.pixeldroid.utils.api.objects

import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_TOOT_CHARS

data class Instance (
    val description: String?,
    val email: String?,
    val max_toot_chars: String? = DEFAULT_MAX_TOOT_CHARS.toString(),
    val registrations: Boolean?,
    val thumbnail: String?,
    val title: String?,
    val uri: String?,
    val version: String?
)