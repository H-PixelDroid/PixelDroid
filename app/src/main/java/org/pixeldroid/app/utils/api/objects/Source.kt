package org.pixeldroid.app.utils.api.objects

import java.io.Serializable

data class Source(
    val note: String?,
    val fields: List<Field>?,
    //Nullable attributes
    val privacy: Privacy?,
    val sensitive: Boolean?,
    val language: String?, //ISO 639-1 language two-letter code
    val follow_requests_count: Int?,
): Serializable {
    enum class Privacy: Serializable {
        public, unlisted, private, direct
    }
}
