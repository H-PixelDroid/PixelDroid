package com.h.pixeldroid.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.h.pixeldroid.db.dao.*
import com.h.pixeldroid.db.dao.feedContent.NotificationDao
import com.h.pixeldroid.db.dao.feedContent.posts.HomePostDao
import com.h.pixeldroid.db.dao.feedContent.posts.PublicPostDao
import com.h.pixeldroid.db.entities.HomeStatusDatabaseEntity
import com.h.pixeldroid.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.db.entities.PublicFeedStatusDatabaseEntity
import com.h.pixeldroid.db.entities.UserDatabaseEntity
import com.h.pixeldroid.objects.Notification

@Database(entities = [
        InstanceDatabaseEntity::class,
        UserDatabaseEntity::class,
        HomeStatusDatabaseEntity::class,
        PublicFeedStatusDatabaseEntity::class,
        Notification::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    abstract fun userDao(): UserDao
    abstract fun homePostDao(): HomePostDao
    abstract fun publicPostDao(): PublicPostDao
    abstract fun notificationDao(): NotificationDao
}