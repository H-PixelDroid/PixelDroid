package org.pixeldroid.app.utils.api.objects

data class PixelfedAppSettings(
    val id: String,
    val username: String,
    val updated_at: String,
    val common: Common
)
data class CommonWrapper(
    val common: Common
)

data class Common(
    val timelines: Timelines,
    val media: Media,
    val appearance: Appearance
)

data class Timelines(
    val show_public: Boolean,
    val show_network: Boolean,
    val hide_likes_shares: Boolean
)

data class Media(
    val hide_public_behind_cw: Boolean,
    val always_show_cw: Boolean,
    val show_alt_text: Boolean
)

data class Appearance(
    val links_use_in_app_browser: Boolean,
    val theme: String
)