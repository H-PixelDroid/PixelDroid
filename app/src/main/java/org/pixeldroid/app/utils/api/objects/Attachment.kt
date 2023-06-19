package org.pixeldroid.app.utils.api.objects

import java.io.Serializable

data class Attachment(
    //Required attributes
    val id: String?,
    val type: AttachmentType? = AttachmentType.image,
    val url: String?, //URL
    val preview_url: String? = "", //URL
    //Optional attributes
    val remote_url: String? = null, //URL

    val meta: Meta?,

    val description: String? = null,
    val blurhash: String? = null,

    //Deprecated attributes
    val text_url: String? = null, //URL

    //Pixelfed's Story upload response... TODO make the server return a regular Attachment?
    val msg: String?,
    val media_id: String?,
    val media_url: String?,
    val media_type: String?,
) : Serializable {
    enum class AttachmentType: Serializable {
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

    val previewNoPlaceholder: String?
        get() = if (preview_url?.contains(Regex("no-preview\\.(png|jpg|webp)$")) == true) url else preview_url

}
