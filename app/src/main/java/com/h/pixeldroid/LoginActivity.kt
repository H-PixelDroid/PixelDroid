package com.h.pixeldroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Application
import com.h.pixeldroid.objects.Token
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val OAUTH_SCHEME = "oauth2redirect"
    private val PACKAGE_ID = "com.h.pixeldroid"
    private val SCOPE = "read write follow"
    private val APP_NAME = "PixelDroid"
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        connect_instance_button.setOnClickListener { onClickConnect() }

        preferences = getSharedPreferences(
            "$PACKAGE_ID.pref", Context.MODE_PRIVATE
        )


    }

    override fun onStart(){
        super.onStart()

        val url = intent.data

        if (url != null && url.toString().startsWith("$OAUTH_SCHEME://$PACKAGE_ID")) {

            val code = url.getQueryParameter("code")
            val error = url.getQueryParameter("error")

            // Restore previous values from preferences
            val domain = preferences.getString("domain", "")
            val clientId = preferences.getString("clientID", "")
            val clientSecret = preferences.getString("clientSecret", "")

            if (code != null && !domain.isNullOrEmpty() && !clientId.isNullOrEmpty() && !clientSecret.isNullOrEmpty()) {
                //Successful authorization
                val callback = object : Callback<Token> {
                    override fun onResponse(call: Call<Token>, response: Response<Token>) {
                        if (response.isSuccessful) {
                            authenticationSuccessful(domain, response.body()?.access_token)
                        } else {
                            failedRegistration("Error getting token")
                        }
                    }

                    override fun onFailure(call: Call<Token>, t: Throwable) {
                        failedRegistration("Error getting token")
                    }
                }

                val pixelfedAPI = PixelfedAPI.create("https://$domain")
                pixelfedAPI.obtainToken(clientId, clientSecret, "$OAUTH_SCHEME://$PACKAGE_ID", SCOPE, code,
                    "authorization_code").enqueue(callback)
            } else if (error != null) {
                failedRegistration("Authentication denied")
            } else {
                failedRegistration("Unknown response")
            }
        }
    }

    private fun authenticationSuccessful(domain: String, accessToken: String?) {

        Log.e("Token", accessToken!!)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun onClickConnect() {

        connect_instance_button.isEnabled = false

        val domain = editText.text.toString()

        val normalizedDomain = normalizeDomain(domain)
        try{
            HttpUrl.Builder().host(domain).scheme("https").build()
        } catch (e: IllegalArgumentException) {
            failedRegistration("Invalid domain")
            return
        }

        preferences.edit()
            .putString("domain", normalizedDomain)
            .apply()

        val callback = object : Callback<Application> {
            override fun onResponse(call: Call<Application>, response: Response<Application>) {
                if (!response.isSuccessful) {
                    failedRegistration()
                    return
                }

                val credentials = response.body()
                val clientId = credentials?.client_id ?: return failedRegistration()
                val clientSecret = credentials.client_secret


                preferences.edit()
                    .putString("clientID", clientId)
                    .putString("clientSecret", clientSecret)
                    .apply()

                redirect(normalizedDomain, clientId)
            }

            override fun onFailure(call: Call<Application>, t: Throwable) {
                failedRegistration()
                return
            }
        }

        val pixelfedAPI = PixelfedAPI.create("https://$normalizedDomain")
        pixelfedAPI.registerApplication(
                APP_NAME,
                "$OAUTH_SCHEME://$PACKAGE_ID", SCOPE
            )
            .enqueue(callback)

    }

    private fun failedRegistration(message: String =
                                       "Could not register the application with this server"){
        connect_instance_button.isEnabled = true
        editText.error = message
    }
    private fun normalizeDomain(domain: String): String {
        var d = domain.replace("http://", "")
        d = d.replace("https://", "")
        return d.trim(Char::isWhitespace)
    }

    fun redirect(normalizedDomain: String, client_id: String) {

        val url = "https://$normalizedDomain/oauth/authorize?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "$OAUTH_SCHEME://$PACKAGE_ID" + "&" +
                "response_type=code" + "&" +
                "scope=$SCOPE"

        browser(this, url)
    }

    private fun browser(context: Context, url: String) {
        val intent = CustomTabsIntent.Builder().build()

        try {
            intent.launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (browserIntent.resolveActivity(packageManager) != null) {
                startActivity(browserIntent)
            } else {
                failedRegistration(message="Could not launch a browser, do you have one?")
                return
            }
        }
        connect_instance_button.isEnabled = true
    }
}
