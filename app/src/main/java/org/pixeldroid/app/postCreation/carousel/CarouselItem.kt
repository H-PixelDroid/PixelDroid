package org.pixeldroid.app.postCreation.carousel

import android.net.Uri

data class CarouselItem constructor(
        val imageUrl: Uri,
        val caption: String? = null
) {
    constructor(imageUrl: Uri) : this(imageUrl, null)
}