package com.h.pixeldroid.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import com.google.android.material.badge.BadgeDrawable
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.objects.Notification
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class NotificationUtils {
    companion object {
        /**
         * @brief updates the amount shown on the notifications badge
         */
        fun updateNotificationsBadge(
            api : PixelfedAPI,
            credential : String,
            context: Context,
            resources: Resources,
            db : AppDatabase,
            badge : BadgeDrawable
        ) {
            //Check for the latest notification id
            api.notifications(credential, limit="1").enqueue(object : Callback<List<Notification>> {
                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                    if (response.code() == 200) {
                        val resultingId = (response.body() as List<Notification>)[0].id.toInt()
                        Log.e("NOTIFICATIONS_ID", "$resultingId")
                        //Check the resulting ID against the db id
                        val oldId = db.userDao().getLatestNotificationId() ?: resultingId

                        Log.e("DB_NOTIF_ID", "$oldId")
                        if(resultingId != oldId) {
                            badge.isVisible = true
                            val difference = resultingId - oldId
                            badge.number = difference
                        } else {
                            badge.isVisible = false
                        }
                    } else{
                        Log.e("NOTIFICATIONS_REQUEST", "${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    Toast.makeText(context, resources.getString(R.string.feed_failed), Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }

        fun updateLatestIdDBEntry(
            api : PixelfedAPI,
            credential : String,
            context: Context,
            resources: Resources,
            db : AppDatabase,
            badge : BadgeDrawable
        ) {
            //Check for the latest notification id
            api.notifications(credential, limit="1").enqueue(object : Callback<List<Notification>> {
                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                    if (response.code() == 200) {
                        val resultingId = (response.body() as List<Notification>)[0].id.toInt()

                        //Stop showing the badge
                        badge.isVisible = false

                        //Update the database entry
                        db.userDao().setLatestNotificationId(resultingId)

                        Log.e("NOTIFICATIONS_LATEST_ID", "$resultingId")
                    } else{
                        Log.e("NOTIFICATIONS_REQUEST", "${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    Toast.makeText(context, resources.getString(R.string.feed_failed), Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }
    }
}