package com.h.pixeldroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Application
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

    }

    fun connexion(view: View) {

        val callback = object : Callback<Application> {
            override fun onResponse(call: Call<Application>, response: Response<Application>) {
                if (!response.isSuccessful) {
                    // TODO
                }

                val credentials = response.body()
                val clientId = credentials!!.client_id
                val clientSecret = credentials.client_secret
                val domain = editText.text.toString()

                preferences = getSharedPreferences(
                    "com.h.PixelDroid.pref", Context.MODE_PRIVATE
                )
                preferences.edit().putString("clientID", clientId)
                    .putString("clientSecret", clientSecret)
                    .putString("domain", domain)
                    .apply()

                if (clientId != null) {
                    redirect(domain, clientId)
                }
            }

            override fun onFailure(call: Call<Application>, t: Throwable) {
            }
        }

        val api = PixelfedAPI.create("https://pixelfed.de")
        api.registerApplication(
                "pixeldroid",
                "oauth2redirect://com.h.pixeldroid", "read write follow"
            )
            .enqueue(callback)

    }

    fun redirect(domain: String, client_id: String) {

        val url = "https://" + domain + "/oauth/authorize" + "?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "oauth2redirect://com.h.pixeldroid" + "&" +
                "response_type=code" + "&" +
                "scope=read write follow"

        browser(this, url)
    }

    fun browser(context: Context, url: String) {
        val intent = CustomTabsIntent.Builder().build()


        try {
            intent.launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            Log.w("login", "Could not launch browser")
        }
    }
}
