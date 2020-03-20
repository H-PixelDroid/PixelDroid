package com.h.pixeldroid.db

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date

@Entity(tableName= "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "domain") val domain: String? = "",
    @ColumnInfo(name = "username") val username: String? = "",
    @ColumnInfo(name = "display name") val displayName: String? = "",
    @ColumnInfo(name = "description") val description: String? = "",
    @ColumnInfo(name = "accountID") val accountID: String? = null,
    @ColumnInfo(name = "nbLikes") val nbLikes: Int? = null,
    @ColumnInfo(name = "nbShares") val nbShares: Int? = null,
    @ColumnInfo(name = "image url") val ImageURL: String? = "",
    @ColumnInfo(name = "profile image url") val profileImgUrl: String? = "",
    @ColumnInfo(name = "date") val date: Date?
)