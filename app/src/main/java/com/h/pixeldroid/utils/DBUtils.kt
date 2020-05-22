package com.h.pixeldroid.utils

import android.content.Context
import androidx.room.Room
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.PostDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.Utils.Companion.normalizeDomain

class DBUtils {
    companion object {
        fun initDB(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "pixeldroid"
            ).allowMainThreadQueries().build()
        }
        private fun normalizeOrNot(uri: String): String{
            return if(uri.startsWith("http://localhost")){
                uri
            } else {
                normalizeDomain(uri)
            }
        }

        fun addUser(db: AppDatabase, account: Account, instance_uri: String, activeUser: Boolean = true, accessToken: String) {
            db.userDao().insertUser(
                    UserDatabaseEntity(
                        user_id = account.id,
                        //make sure not to normalize to https when localhost, to allow testing
                        instance_uri = normalizeOrNot(instance_uri),
                        username = account.username,
                        display_name = account.display_name,
                        avatar_static = account.avatar_static,
                        isActive = activeUser,
                        accessToken = accessToken
                    )
                )
        }

        fun storeInstance(db: AppDatabase, instance: Instance) {
            val maxTootChars = instance.max_toot_chars.toInt()
            val dbInstance = InstanceDatabaseEntity(
                //make sure not to normalize to https when localhost, to allow testing
                uri = normalizeOrNot(instance.uri),
                title = instance.title,
                max_toot_chars = maxTootChars,
                thumbnail = instance.thumbnail
            )
            db.instanceDao().insertInstance(dbInstance)
        }

        fun storePosts(db: AppDatabase, data: List<*>) {
            if (data.firstOrNull() is Status) {
                for (post in data) {
                    val post = post as Status
                    if (true) {
                        val postDBEntity = PostDatabaseEntity(
                            account_profile_picture = "",
                            account_name = "",
                            picture = "",
                            favourite_count = 0,
                            reply_count = 0
                        )
                    }
                }
            }
        }
    }
}