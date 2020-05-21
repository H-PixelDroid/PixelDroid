package com.h.pixeldroid.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InstanceDatabaseEntity::class, UserDatabaseEntity::class, NotificationIdDatabaseEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instanceDao(): InstanceDao
    abstract fun userDao(): UserDao
    abstract fun notificationIdDao() : NotificationIdDao
}