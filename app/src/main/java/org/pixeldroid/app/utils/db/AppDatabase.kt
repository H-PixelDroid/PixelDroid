package org.pixeldroid.app.utils.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.pixeldroid.app.utils.api.objects.Conversation
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.db.dao.InstanceDao
import org.pixeldroid.app.utils.db.dao.TabsDao
import org.pixeldroid.app.utils.db.dao.UserDao
import org.pixeldroid.app.utils.db.dao.feedContent.DirectMessagesConversationDao
import org.pixeldroid.app.utils.db.dao.feedContent.DirectMessagesDao
import org.pixeldroid.app.utils.db.dao.feedContent.NotificationDao
import org.pixeldroid.app.utils.db.dao.feedContent.posts.HomePostDao
import org.pixeldroid.app.utils.db.dao.feedContent.posts.PublicPostDao
import org.pixeldroid.app.utils.db.entities.DirectMessageDatabaseEntity
import org.pixeldroid.app.utils.db.entities.HomeStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.PublicFeedStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity

@Database(entities = [
        InstanceDatabaseEntity::class,
        UserDatabaseEntity::class,
        HomeStatusDatabaseEntity::class,
        PublicFeedStatusDatabaseEntity::class,
        Notification::class,
        TabsDatabaseEntity::class,
        Conversation::class,
        DirectMessageDatabaseEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ],
    version = 10
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    abstract fun userDao(): UserDao
    abstract fun homePostDao(): HomePostDao
    abstract fun publicPostDao(): PublicPostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun tabsDao(): TabsDao
    abstract fun directMessagesDao(): DirectMessagesDao
    abstract fun directMessagesConversationDao(): DirectMessagesConversationDao
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM homePosts")
        database.execSQL("DELETE FROM publicPosts")
        database.execSQL("DELETE FROM notifications")
    }
}
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE instances ADD COLUMN videoEnabled INTEGER NOT NULL DEFAULT 1")
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE instances ADD COLUMN pixelfed INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Sorry users, this was just easier
        database.execSQL("DELETE FROM tabsChecked")
    }
}