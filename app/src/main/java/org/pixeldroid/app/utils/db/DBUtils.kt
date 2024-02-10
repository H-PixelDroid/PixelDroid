package org.pixeldroid.app.utils.db

import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Instance
import org.pixeldroid.app.utils.api.objects.NodeInfo
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_ALBUM_LIMIT
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_PHOTO_SIZE
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_TOOT_CHARS
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_MAX_VIDEO_SIZE
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity.Companion.DEFAULT_VIDEO_ENABLED
import org.pixeldroid.app.utils.normalizeDomain
import java.lang.IllegalArgumentException

suspend fun addUser(
    db: AppDatabase, account: Account, instance_uri: String, activeUser: Boolean = true,
    accessToken: String, refreshToken: String?, clientId: String, clientSecret: String,
) {
    db.userDao().insertOrUpdate(
        UserDatabaseEntity(
            user_id = account.id!!,
            instance_uri = normalizeDomain(instance_uri),
            username = account.username!!,
            display_name = account.getDisplayName(),
            avatar_static = account.anyAvatar().orEmpty(),
            isActive = activeUser,
            accessToken = accessToken,
            refreshToken = refreshToken,
            clientId = clientId,
            clientSecret = clientSecret
        )
    )
}

suspend fun updateUserInfoDb(db: AppDatabase, account: Account) {
    val user = db.userDao().getActiveUser()!!
    db.userDao().updateUserAccountDetails(
        account.username.orEmpty(),
        account.display_name.orEmpty(),
        account.anyAvatar().orEmpty(),
        user.user_id,
        user.instance_uri
    )
}

suspend fun storeInstance(db: AppDatabase, nodeInfo: NodeInfo?, instance: Instance? = null) {
    val dbInstance: InstanceDatabaseEntity = nodeInfo?.run {
        InstanceDatabaseEntity(
            uri = normalizeDomain(metadata?.config?.site?.url!!),
            title = metadata.config.site.name!!,
            maxStatusChars = metadata.config.uploader?.max_caption_length!!.toInt(),
            maxPhotoSize = metadata.config.uploader.max_photo_size?.toIntOrNull()
                ?: DEFAULT_MAX_PHOTO_SIZE,
            // Pixelfed doesn't distinguish between max photo and video size
            maxVideoSize = metadata.config.uploader.max_photo_size?.toIntOrNull()
                ?: DEFAULT_MAX_VIDEO_SIZE,
            albumLimit = metadata.config.uploader.album_limit?.toIntOrNull() ?: DEFAULT_ALBUM_LIMIT,
            videoEnabled = metadata.config.features?.video ?: DEFAULT_VIDEO_ENABLED,
            pixelfed = metadata.software?.repo?.contains("pixelfed", ignoreCase = true) == true
        )
    } ?: instance?.run {
        InstanceDatabaseEntity(
            uri = normalizeDomain(uri.orEmpty()),
            title = title.orEmpty(),
            maxStatusChars = max_toot_chars?.toInt() ?: DEFAULT_MAX_TOOT_CHARS,
            pixelfed = false
        )
    } ?: throw IllegalArgumentException("Cannot store instance where both are null")

    db.instanceDao().insertOrUpdate(dbInstance)
}