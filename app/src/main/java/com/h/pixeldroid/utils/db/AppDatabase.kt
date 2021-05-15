package com.h.pixeldroid.utils.db

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.h.pixeldroid.utils.db.dao.*
import com.h.pixeldroid.utils.db.dao.feedContent.NotificationDao
import com.h.pixeldroid.utils.db.dao.feedContent.posts.HomePostDao
import com.h.pixeldroid.utils.db.dao.feedContent.posts.PublicPostDao
import com.h.pixeldroid.utils.db.entities.HomeStatusDatabaseEntity
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.PublicFeedStatusDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Notification

@Database(entities = [
        InstanceDatabaseEntity::class,
        UserDatabaseEntity::class,
        HomeStatusDatabaseEntity::class,
        PublicFeedStatusDatabaseEntity::class,
        Notification::class
    ],
    version = 3
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    abstract fun userDao(): UserDao
    abstract fun homePostDao(): HomePostDao
    abstract fun publicPostDao(): PublicPostDao
    abstract fun notificationDao(): NotificationDao
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE instances ADD COLUMN maxPhotoSize INTEGER NOT NULL DEFAULT 8000")
        database.execSQL("ALTER TABLE instances RENAME COLUMN max_toot_chars TO maxStatusChars")
        database.execSQL("ALTER TABLE instances ADD COLUMN maxVideoSize INTEGER NOT NULL DEFAULT 40000")
        database.execSQL("ALTER TABLE instances ADD COLUMN albumLimit INTEGER NOT NULL DEFAULT 4")
    }
}