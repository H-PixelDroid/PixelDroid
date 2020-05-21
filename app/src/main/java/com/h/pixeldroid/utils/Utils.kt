package com.h.pixeldroid.utils

import android.content.Context
import android.net.ConnectivityManager

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


    }
}