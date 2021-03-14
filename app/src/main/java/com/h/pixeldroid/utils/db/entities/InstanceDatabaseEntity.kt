package com.h.pixeldroid.utils.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.h.pixeldroid.utils.api.objects.Instance

@Entity(tableName = "instances")
data class InstanceDatabaseEntity (
        @PrimaryKey var uri: String,
        var title: String,
        var maxStatusChars: Int = DEFAULT_MAX_TOOT_CHARS,
        // Per-file file-size limit in KB. Defaults to 15000 (15MB). Default limit for Mastodon is 8MB
        var maxPhotoSize: Int = DEFAULT_MAX_PHOTO_SIZE,
        // Mastodon has different file limits for videos, default of 40MB
        var maxVideoSize: Int = DEFAULT_MAX_VIDEO_SIZE,
        // How many photos can go into an album. Default limit for Pixelfed and Mastodon is 4
        var albumLimit: Int = DEFAULT_ALBUM_LIMIT,
) {
    companion object{
        // Default max number of chars for Mastodon: used when their is no other value supplied by
        // either NodeInfo or the instance endpoint
        const val DEFAULT_MAX_TOOT_CHARS = 500

        const val DEFAULT_MAX_PHOTO_SIZE = 8000
        const val DEFAULT_MAX_VIDEO_SIZE = 40000
        const val DEFAULT_ALBUM_LIMIT = 4
    }
}