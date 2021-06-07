package org.pixeldroid.app.utils.api.objects

import java.io.Serializable

data class History(
    //Required attributes
    val day: String,
    val uses: String,
    val accounts: String
): Serializable
