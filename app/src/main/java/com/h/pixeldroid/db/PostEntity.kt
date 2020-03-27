package com.h.pixeldroid.db

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.h.pixeldroid.objects.*
import java.util.Date

@Entity(tableName= "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long,
    @ColumnInfo(name = "status") val status: Status,
    @ColumnInfo(name = "date") val date: Date?
)