package com.h.pixeldroid.db

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun listToJson(list: List<String>): String = Gson().toJson(list)

     @TypeConverter
     fun jsonToList(json: String): List<String> =
         Gson().fromJson(json, Array<String>::class.java).toList()
}