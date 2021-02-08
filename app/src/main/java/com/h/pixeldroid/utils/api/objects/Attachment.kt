package com.h.pixeldroid.utils.api.objects

import java.io.Serializable

data class Attachment(
    //Required attributes
    val id: String?,
    val type: AttachmentType? = AttachmentType.image,
    val url: String?, //URL
    val preview_url: String? = "", //URL
    //Optional attributes
    val remote_url: String? = null, //URL
    val text_url: String? = null, //URL

    val meta: Meta?,

    val description: String? = null,
    val blurhash: String? = null
) : Serializable {
    enum class AttachmentType {
        unknown, image, gifv, video, audio
    }

    data class Meta (
            val focus: Focus?,
            val original: Image?
    ) : Serializable

    {
        data class Focus(
                val x: Double?,
                val y: Double?
        ) : Serializable
        data class Image(
                val width: Int?,
                val height: Int?,
                val size: String?,
                val aspect: Double?
        ) : Serializable
    }
}
