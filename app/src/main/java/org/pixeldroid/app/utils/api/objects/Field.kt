package org.pixeldroid.app.utils.api.objects

import java.io.Serializable
import java.time.Instant

data class Field(
    //Required attributes
    val name: String?,
    val value: String?,
    //Optional attributes
    val verified_at: Instant?
): Serializable
