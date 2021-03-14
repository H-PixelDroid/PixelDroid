package com.h.pixeldroid.utils.db

import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Instance
import com.h.pixeldroid.utils.api.objects.NodeInfo
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_ALBUM_LIMIT
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_PHOTO_SIZE
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_TOOT_CHARS
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_VIDEO_SIZE
import com.h.pixeldroid.utils.normalizeDomain
import java.lang.IllegalArgumentException

fun addUser(db: AppDatabase, account: Account, instance_uri: String, activeUser: Boolean = true,
            accessToken: String, refreshToken: String?, clientId: String, clientSecret: String) {
    db.userDao().insertUser(
            UserDatabaseEntity(
                    user_id = account.id!!,
                    instance_uri = normalizeDomain(instance_uri),
                    username = account.username!!,
                    display_name = account.getDisplayName(),
                    avatar_static = account.avatar_static.orEmpty(),
                    isActive = activeUser,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    clientId = clientId,
                    clientSecret = clientSecret
            )
    )
}

fun storeInstance(db: AppDatabase, nodeInfo: NodeInfo?, instance: Instance? = null) {
    val dbInstance: InstanceDatabaseEntity = nodeInfo?.run {
        InstanceDatabaseEntity(
                uri = normalizeDomain(metadata?.config?.site?.url!!),
                title = metadata.config.site.name!!,
                maxStatusChars = metadata.config.uploader?.max_caption_length!!.toInt(),
                maxPhotoSize = metadata.config.uploader.max_photo_size?.toIntOrNull() ?: DEFAULT_MAX_PHOTO_SIZE,
                //Pixelfed doesn't distinguish between max photo and video size
                maxVideoSize = metadata.config.uploader.max_photo_size?.toIntOrNull() ?: DEFAULT_MAX_VIDEO_SIZE,
                albumLimit = metadata.config.uploader.album_limit?.toIntOrNull() ?: DEFAULT_ALBUM_LIMIT
        )
    } ?: instance?.run {
        InstanceDatabaseEntity(
                uri = normalizeDomain(uri.orEmpty()),
                title = title.orEmpty(),
                maxStatusChars = max_toot_chars?.toInt() ?: DEFAULT_MAX_TOOT_CHARS,
        )
    } ?: throw IllegalArgumentException("Cannot store instance where both are null")

    db.instanceDao().insertInstance(dbInstance)
}