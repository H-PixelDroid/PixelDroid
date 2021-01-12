package com.h.pixeldroid.postCreation.carousel

data class CarouselItem constructor(
    val imageUrl: String? = null,
    val caption: String? = null
) {
    constructor(imageUrl: String? = null) : this(imageUrl, null)
}