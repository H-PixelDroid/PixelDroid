package com.h.pixeldroid.utils

import android.content.Context
import androidx.room.Room
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.objects.Account

class DBUtils {
    companion object {
        fun initDB(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "pixeldroid"
            ).allowMainThreadQueries().build()
        }

        fun addUser(db: AppDatabase, account: Account, instance_uri: String, activeUser: Boolean = true, accessToken: String) {
                db.userDao().insertUser(
                    UserDatabaseEntity(
                        user_id = account.id,
                        instance_uri = instance_uri,
                        username = account.username,
                        display_name = account.display_name,
                        avatar_static = account.avatar_static,
                        isActive = activeUser,
                        accessToken = accessToken
                    )
                )
        }
    }
}