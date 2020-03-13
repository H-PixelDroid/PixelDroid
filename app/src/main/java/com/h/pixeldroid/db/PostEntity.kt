package com.h.pixeldroid.db

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date

@Entity(tableName= "posts")
data class PostEntity(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "domain") val domain: String? = "",
    @ColumnInfo(name = "username") val username: String? = "",
    @ColumnInfo(name = "display name") val displayName: String? = "",
    @ColumnInfo(name = "accountID") val accountID: Int? = -1,
    @ColumnInfo(name = "image url") val ImageURL: String? = "",
    @ColumnInfo(name = "date") val date: Date?
)