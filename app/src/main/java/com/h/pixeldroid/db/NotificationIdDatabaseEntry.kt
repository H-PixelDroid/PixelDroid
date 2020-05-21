package com.h.pixeldroid.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "latestNotification")
data class NotificationIdDatabaseEntry (
    @PrimaryKey var notificationId: Int
)