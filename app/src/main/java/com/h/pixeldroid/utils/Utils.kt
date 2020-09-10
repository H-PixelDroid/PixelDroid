package com.h.pixeldroid.utils

import android.content.Context
import android.net.ConnectivityManager
import android.widget.TextView
import com.h.pixeldroid.R
import java.text.ParseException
import java.util.Date

class Utils {
    companion object {
        fun hasInternet(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetwork != null
        }

        fun normalizeDomain(domain: String): String {
            return "https://" + domain
                .replace("http://", "")
                .replace("https://", "")
                .trim(Char::isWhitespace)
        }


        fun setTextViewFromISO8601(date: Date, textView: TextView, absoluteTime: Boolean, context: Context) {
            val now = Date().time

            try {
                val then = date.time
                val formattedDate = android.text.format.DateUtils
                    .getRelativeTimeSpanString(then, now,
                        android.text.format.DateUtils.SECOND_IN_MILLIS,
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE)

                textView.text = if(absoluteTime) context.getString(R.string.posted_on).format(date)
                else "$formattedDate"

            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
    }
}