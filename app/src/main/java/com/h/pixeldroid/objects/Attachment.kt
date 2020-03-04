package com.h.pixeldroid.objects

data class Attachment(
    //Required attributes
    val id: String,
    val type: AttachmentType,
    val url: String, //URL
    val preview_url: String, //URL
    //Optional attributes
    val remote_url: String? = null, //URL
    val text_url: String? = null, //URL
    //TODO meta
    val description: String? = null,
    val blurhash: String? = null
) {
    enum class AttachmentType {
        unknown, image, gifv, video, audio
    }
}
