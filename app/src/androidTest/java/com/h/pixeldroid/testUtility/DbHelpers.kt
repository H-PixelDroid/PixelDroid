package com.h.pixeldroid.testUtility

import android.content.Context
import androidx.room.Room
import com.h.pixeldroid.db.AppDatabase

fun initDB(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java, "pixeldroid"
    ).allowMainThreadQueries().build()
}
