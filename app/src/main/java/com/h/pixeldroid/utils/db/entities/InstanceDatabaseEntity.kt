package com.h.pixeldroid.utils.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.h.pixeldroid.utils.api.objects.Instance

@Entity(tableName = "instances")
data class InstanceDatabaseEntity (
        @PrimaryKey var uri: String,
        var title: String,
        var maxStatusChars: Int = Instance.DEFAULT_MAX_TOOT_CHARS,

)