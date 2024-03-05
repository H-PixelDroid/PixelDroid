package org.pixeldroid.app.postCreation.carousel

import android.net.Uri

data class CarouselItem constructor(
        val imageUrl: Uri,
        val caption: String? = null,
        val video: Boolean,
        var encodeProgress: Int?,
        var stabilizationFirstPass: Boolean?,
        var encodeComplete: Boolean? = null,
        var encodeError: Boolean = false,
)