package org.pixeldroid.app.utils.api.objects

import java.io.Serializable
import java.time.Instant

data class Poll (
    val id: String?,
    val expires_at: Instant? = null, //ISO 8601 Datetime, or null if poll does not end
    val expired: Boolean?,
    val multiple: Boolean, //Does the poll allow multiple-choice answers?
    val votes_count: Int?,
    val voters_count: Int?,
    val voted: Boolean?, //null if gotten without user token
    val own_votes: List<Int?>?,
    val options: List<Option?>?,
    val emojis: List<Emoji?>?
    ): Serializable {
        data class Option(
            val title: String?,
            val votes_count: Int? //null if result not published yet
        )
    }
