package com.h.pixeldroid.utils

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.PostEntity
import com.h.pixeldroid.db.PostViewModel
import com.h.pixeldroid.objects.*
import java.io.Serializable
import java.util.Calendar

class DatabaseUtils {
    companion object {
        fun postEntityListToStatusList(allPosts: ArrayList<PostEntity>) : ArrayList<Status> {
            val allStatus: ArrayList<Status> = arrayListOf()

            allPosts.forEach() {
                allStatus.add(it.status)
            }

            return allStatus
        }
    }
}