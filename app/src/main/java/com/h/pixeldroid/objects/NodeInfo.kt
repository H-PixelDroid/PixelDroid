package com.h.pixeldroid.objects

/*
    See https://nodeinfo.diaspora.software/schema.html and https://pixelfed.social/api/nodeinfo/2.0.json
    A lot of attributes we don't need are omitted, if in the future they are needed we
    can make new data classes for them.
*/

data class NodeInfo (
    val version: String?,
    val software: Software?,
    val protocols: List<String>?,
    val openRegistrations: Boolean?,
    val metadata: PixelfedMetadata?
){
    data class Software(
        val name: String?,
        val version: String?
    )
    data class PixelfedMetadata(
        val nodeName: String?,
        val software: Software?,
        val config: PixelfedConfig
    ){
        data class Software(
            val homepage: String?,
            val repo: String?
        )
    }
    data class PixelfedConfig(
        val open_registration: Boolean?,
        val uploader: Uploader?,
        val activitypub: ActivityPub?,
        val features: Features?
    ){
        data class Uploader(
            val max_photo_size: String?,
            val max_caption_length: String?,
            val album_limit: String?,
            val image_quality: String?,
            val optimize_image: Boolean?,
            val optimize_video: Boolean?,
            val media_types: String?,
            val enforce_account_limit: Boolean?
        )

        data class ActivityPub(
            val enabled: Boolean?,
            val remote_follow: Boolean?
        )

        data class Features(
            val mobile_apis: Boolean?,
            val circles: Boolean?,
            val stories: Boolean?,
            val video: Boolean?
        )
    }
}

data class NodeInfoJRD(
    val links: List<Link>
){
    data class Link(
        val rel: String?,
        val href: String?
    )

}