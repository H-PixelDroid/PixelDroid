package com.h.pixeldroid.utils.db

import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Instance
import com.h.pixeldroid.utils.normalizeDomain

private fun normalizeOrNot(uri: String): String{
    return if(uri.startsWith("http://localhost")){
        uri
    } else {
        normalizeDomain(uri)
    }
}

fun addUser(db: AppDatabase, account: Account, instance_uri: String, activeUser: Boolean = true,
            accessToken: String, refreshToken: String?, clientId: String, clientSecret: String) {
    db.userDao().insertUser(
            UserDatabaseEntity(
                    user_id = account.id!!,
                    //make sure not to normalize to https when localhost, to allow testing
                    instance_uri = normalizeOrNot(instance_uri),
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

fun storeInstance(db: AppDatabase, instance: Instance) {
    val maxTootChars = instance.max_toot_chars?.toInt() ?: Instance.DEFAULT_MAX_TOOT_CHARS
    val dbInstance = InstanceDatabaseEntity(
            //make sure not to normalize to https when localhost, to allow testing
            uri = normalizeOrNot(instance.uri.orEmpty()),
            title = instance.title.orEmpty(),
            max_toot_chars = maxTootChars,
            thumbnail = instance.thumbnail.orEmpty()
    )
    db.instanceDao().insertInstance(dbInstance)
}