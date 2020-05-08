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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Login Activity"
        private const val PACKAGE_ID = BuildConfig.APPLICATION_ID
        private const val SCOPE = "read write follow"
    }

    private lateinit var oauthScheme: String
    private lateinit var appName: String
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        connect_instance_button.setOnClickListener { registerAppToServer() }
        whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }

        appName = getString(R.string.app_name)
        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences(
            "$PACKAGE_ID.pref", Context.MODE_PRIVATE
        )
    }

    override fun onStart(){
        super.onStart()
        val url: Uri? = intent.data

        //Check if the activity was started after the authentication
        if (url == null || !url.toString().startsWith("$oauthScheme://$PACKAGE_ID")) return
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

    private fun whatsAnInstance() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://pixelfed.org/join")
        startActivity(i)
    }

    private fun normalizeDomain(domain: String): String {
        var d = domain.replace("http://", "")
        d = d.replace("https://", "")
        return d.trim(Char::isWhitespace)
    }

    private fun registerAppToServer() {
        loadingAnimation(true)
        val normalizedDomain = "https://" + normalizeDomain(editText.text.toString())
        PixelfedAPI.create(normalizedDomain).registerApplication(
            appName,"$oauthScheme://$PACKAGE_ID", SCOPE
        ).enqueue(object : Callback<Application> {
            override fun onResponse(call: Call<Application>, response: Response<Application>) {
                if (!response.isSuccessful) {
                    return failedRegistration()
                }
                preferences.edit()
                    .putString("domain", normalizedDomain)
                    .apply()
                val credentials = response.body() as Application
                val clientId = credentials.client_id ?: return failedRegistration()
                preferences.edit()
                    .putString("clientID", clientId)
                    .putString("clientSecret", credentials.client_secret)
                    .apply()
                promptOAuth(normalizedDomain, clientId)
                getInstanceConfig()
            }

            override fun onFailure(call: Call<Application>, t: Throwable) {
                return failedRegistration()
            }
        })
    }

    private fun promptOAuth(normalizedDomain: String, client_id: String) {

        val url = "$normalizedDomain/oauth/authorize?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "$oauthScheme://$PACKAGE_ID" + "&" +
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
        val domain = preferences.getString("domain", "") as String
        val clientId = preferences.getString("clientID", "") as String
        val clientSecret = preferences.getString("clientSecret", "") as String

        if (code.isNullOrEmpty() || domain.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty()) {
            return failedRegistration(getString(R.string.auth_failed))
        }

        //Successful authorization
        val callback = object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (!response.isSuccessful || response.body() == null) {
                    return failedRegistration(getString(R.string.token_error))
                }
                authenticationSuccessful(response.body()!!.access_token)
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                return failedRegistration(getString(R.string.token_error))
            }
        }

        PixelfedAPI.create(domain)
            .obtainToken(
            clientId, clientSecret, "$oauthScheme://$PACKAGE_ID", SCOPE, code,
            "authorization_code"
        ).enqueue(callback)
    }

    private fun authenticationSuccessful(accessToken: String) {
        preferences.edit().putString("accessToken", accessToken).apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun failedRegistration(message: String = getString(R.string.registration_failed)){
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
