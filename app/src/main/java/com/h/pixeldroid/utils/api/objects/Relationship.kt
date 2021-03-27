package com.h.pixeldroid.utils.api.objects

import java.io.Serializable

data class Relationship(
    // Required atributes
    val id: String,
    val following: Boolean?,
    val requested: Boolean?,
    val endorsed: Boolean?,
    val followed_by: Boolean?,
    val muting: Boolean?,
    val muting_notifications: Boolean?,
    val showing_reblogs: Boolean?,
    val blocking: Boolean?,
    val domain_blocking: Boolean?,
    val blocked_by: Boolean?
) : Serializable
