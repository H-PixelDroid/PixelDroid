package com.h.pixeldroid.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.h.pixeldroid.objects.Status
import java.util.Date

class Converters {
    val gson = Gson()

    @TypeConverter
    fun timestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

    @TypeConverter
    fun statusToJson(status: Status?): String {
        return gson.toJson(status)
    }

    @TypeConverter
    fun jsonToStatus(status: String?): Status? {
        return gson.fromJson(status, Status::class.java)
    }
}
