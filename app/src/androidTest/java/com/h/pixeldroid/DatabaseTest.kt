package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.utils.DBUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = DBUtils.initDB(context)
        db.clearAllTables()
    }

}