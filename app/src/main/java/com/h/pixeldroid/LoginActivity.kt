package com.h.pixeldroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Application
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.objects.Token
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val TAG = "Login Activity"

    private lateinit var OAUTH_SCHEME: String
    private val PACKAGE_ID = BuildConfig.APPLICATION_ID
    private val SCOPE = "read write follow"
    private lateinit var APP_NAME: String
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        connect_instance_button.setOnClickListener { onClickConnect() }
        APP_NAME = getString(R.string.app_name)
        OAUTH_SCHEME = getString(R.string.auth_scheme)
        preferences = getSharedPreferences(
            "$PACKAGE_ID.pref", Context.MODE_PRIVATE
        )
    }

    override fun onStart(){
        super.onStart()

        val url = intent.data

        //Check if the activity was started after the authentication
        if (url == null || !url.toString().startsWith("$OAUTH_SCHEME://$PACKAGE_ID")) return

        loadingAnimation(true)

        val code = url.getQueryParameter("code")
        authenticate(code)

    }

    override fun onStop() {
        super.onStop()
        loadingAnimation(false)
    }

    override fun onBackPressed() {
    }

    private fun onClickConnect() {

        val normalizedDomain = normalizeDomain(editText.text.toString())

        try{
            HttpUrl.Builder().host(normalizedDomain).scheme("https").build()
        } catch (e: IllegalArgumentException) {
            return failedRegistration(getString(R.string.invalid_domain))
        }

        loadingAnimation(true)

        preferences.edit()
            .putString("domain", "https://$normalizedDomain")
            .apply()
        registerAppToServer("https://$normalizedDomain")

    }

    private fun normalizeDomain(domain: String): String {
        var d = domain.replace("http://", "")
        d = d.replace("https://", "")
        return d.trim(Char::isWhitespace)
    }

    private fun registerAppToServer(normalizedDomain: String) {
        val callback = object : Callback<Application> {
            override fun onResponse(call: Call<Application>, response: Response<Application>) {
                if (!response.isSuccessful) {
                    return failedRegistration()
                }

                val credentials = response.body()
                val clientId = credentials?.client_id ?: return failedRegistration()
                val clientSecret = credentials.client_secret

                preferences.edit()
                    .putString("clientID", clientId)
                    .putString("clientSecret", clientSecret)
                    .apply()

                promptOAuth(normalizedDomain, clientId)
            }

            override fun onFailure(call: Call<Application>, t: Throwable) {
                return failedRegistration()
            }
        }
        PixelfedAPI.create(normalizedDomain).registerApplication(
            APP_NAME,"$OAUTH_SCHEME://$PACKAGE_ID", SCOPE
        ).enqueue(callback)
    }

    private fun promptOAuth(normalizedDomain: String, client_id: String) {

        val url = "$normalizedDomain/oauth/authorize?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "$OAUTH_SCHEME://$PACKAGE_ID" + "&" +
                "response_type=code" + "&" +
                "scope=$SCOPE"

        val intent = CustomTabsIntent.Builder().build()

        try {
            intent.launchUrl(this, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (browserIntent.resolveActivity(packageManager) != null) {
                startActivity(browserIntent)
            } else {
                return failedRegistration(getString(R.string.browser_launch_failed))
            }
        }
    }

    private fun authenticate(code: String?) {

        // Get previous values from preferences
        val domain = preferences.getString("domain", "")
        val clientId = preferences.getString("clientID", "")
        val clientSecret = preferences.getString("clientSecret", "")

        if (code == null || domain.isNullOrEmpty() || clientId.isNullOrEmpty() || clientSecret.isNullOrEmpty()) {
            return failedRegistration(getString(R.string.auth_failed))
        }

        //Successful authorization
        val callback = object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (!response.isSuccessful || response.body() == null) {
                    return failedRegistration(getString(R.string.token_error))
                }
                authenticationSuccessful(domain, response.body()!!.access_token)
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                return failedRegistration(getString(R.string.token_error))
            }
        }

        PixelfedAPI.create("$domain")
            .obtainToken(
            clientId, clientSecret, "$OAUTH_SCHEME://$PACKAGE_ID", SCOPE, code,
            "authorization_code"
        ).enqueue(callback)
    }

    private fun authenticationSuccessful(domain: String, accessToken: String) {
        preferences.edit().putString("accessToken", accessToken).apply()
        getInstanceConfig()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun failedRegistration(message: String =
                                       getString(R.string.registration_failed)){
        loadingAnimation(false)
        editText.error = message
    }

    private fun loadingAnimation(on: Boolean){
        if(on) {
            domainTextInputLayout.visibility = View.GONE
            progressLayout.visibility = View.VISIBLE
        }
        else {
            domainTextInputLayout.visibility = View.VISIBLE
            progressLayout.visibility = View.GONE
        }
    }

    private fun getInstanceConfig() {
        // to get max post description length, can be enhanced for other things
        // see /api/v1/instance
        PixelfedAPI.create(preferences.getString("domain", "")!!)
            .instance().enqueue(object : Callback<Instance> {

            override fun onFailure(call: Call<Instance>, t: Throwable) {
                Log.e(TAG, "Request to fetch instance config failed.")
                preferences.edit().putInt("max_toot_chars", 500).apply()
            }

            override fun onResponse(call: Call<Instance>, response: Response<Instance>) {
                if (response.code() == 200) {
                    Log.e(TAG, response.body().toString())
                    preferences.edit().putInt(
                        "max_toot_chars",
                        response.body()!!.max_toot_chars.toInt()
                    ).apply()
                } else {
                    Log.e(TAG, "Server response to fetch instance config failed.")
                    preferences.edit().putInt("max_toot_chars", 500).apply()
                }
            }

        })
    }

}
