package com.h.pixeldroid.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface NotificationIdDao {
    @Query("SELECT * FROM latestNotification")
    fun get(): NotificationIdDatabaseEntry?

    @Query("UPDATE latestNotification SET notificationId = :notificationId")
    fun updateLatestId(notificationId : Int)
}