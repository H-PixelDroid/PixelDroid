package org.pixeldroid.app.testUtility

import android.content.Context
import androidx.room.Room
import org.pixeldroid.app.utils.db.AppDatabase
import org.ligi.tracedroid.TraceDroid


fun initDB(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java, "pixeldroid"
    ).allowMainThreadQueries().build()
}

fun clearData() {
    TraceDroid.deleteStacktraceFiles()
}
