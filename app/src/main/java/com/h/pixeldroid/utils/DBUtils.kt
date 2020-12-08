package com.h.pixeldroid.utils

import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.db.entities.UserDatabaseEntity
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.utils.Utils.Companion.normalizeDomain

class DBUtils {
    companion object {
        private const val MAX_NUMBER_OF_STORED_POSTS = 200

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
    }
}