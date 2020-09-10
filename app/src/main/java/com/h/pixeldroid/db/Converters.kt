package com.h.pixeldroid.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.util.Date

class Converters {
    @TypeConverter
    fun listToJson(list: List<String>): String = Gson().toJson(list)

     @TypeConverter
     fun jsonToList(json: String): List<String> =
         Gson().fromJson(json, Array<String>::class.java).toList()

    @TypeConverter
    fun dateToJson(date: Date): String = Gson().toJson(date)

    @TypeConverter
    fun jsontoDate(json: String): Date = Gson().fromJson(json, Date::class.java)


}