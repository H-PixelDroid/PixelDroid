package org.pixeldroid.app.utils.api.objects

import org.pixeldroid.app.utils.validDomain

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
    val metadata: PixelfedMetadata?,
){
    /**
     * Check if this NodeInfo has the fields we need or if we also need to look into the
     * /api/v1/instance endpoint
     * This only checks for values that might be in the /api/v1/instance endpoint.
     */
    fun hasInstanceEndpointInfo(): Boolean {
        return validDomain(metadata?.config?.site?.url)
                && !metadata?.config?.site?.name.isNullOrBlank()
                && metadata?.config?.uploader?.max_caption_length?.toIntOrNull() != null
    }



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
        val features: Features?,
        val site: Site?
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

        data class Site(
                val name: String?,
                val domain: String?,
                val url: String?,
                val description: String?
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