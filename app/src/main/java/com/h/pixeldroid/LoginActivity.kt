package com.h.pixeldroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Application
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.objects.Token
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.DBUtils.Companion.storeInstance
import com.h.pixeldroid.utils.Utils
import com.h.pixeldroid.utils.Utils.Companion.normalizeDomain
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.internal.toImmutableList
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
    private lateinit var db: AppDatabase
    private lateinit var pixelfedAPI: PixelfedAPI
    private var inputVisibility: Int = View.GONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingAnimation(true)
        appName = getString(R.string.app_name)
        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences("$PACKAGE_ID.pref", Context.MODE_PRIVATE)
        db = DBUtils.initDB(applicationContext)

        if (Utils.hasInternet(applicationContext)) {
            connect_instance_button.setOnClickListener {
                registerAppToServer(normalizeDomain(editText.text.toString()))
            }
            whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }
            inputVisibility = View.VISIBLE
        } else {
            login_activity_connection_required_text.visibility = View.VISIBLE
        }
        loadingAnimation(false)
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


    private fun whatsAnInstance() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://pixelfed.org/join")
        startActivity(i)
    }


    private fun registerAppToServer(normalizedDomain: String) {
        loadingAnimation(true)
        if (normalizedDomain.replace("https://", "").isNullOrBlank())
            return failedRegistration(getString(R.string.login_empty_string_error))
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

        if (code.isNullOrBlank() || domain.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
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

        pixelfedAPI = PixelfedAPI.create(domain)
        pixelfedAPI.obtainToken(
            clientId, clientSecret, "$oauthScheme://$PACKAGE_ID", SCOPE, code,
            "authorization_code"
        ).enqueue(callback)
    }

    private fun authenticationSuccessful(accessToken: String) {
        saveUserAndInstance(accessToken)
        wipeSharedSettings()
    }

    private fun failedRegistration(message: String = getString(R.string.registration_failed)) {
        loadingAnimation(false)
        editText.error = message
        wipeSharedSettings()
    }

    private fun wipeSharedSettings(){
        preferences.edit().remove("domain").remove("clientId").remove("clientSecret")
            .apply()
    }

    private fun loadingAnimation(on: Boolean){
        if(on) {
            login_activity_instance_input_layout.visibility = View.GONE
            progressLayout.visibility = View.VISIBLE
        }
        else {
            login_activity_instance_input_layout.visibility = inputVisibility
            progressLayout.visibility = View.GONE
        }
    }

    private fun saveUserAndInstance(accessToken: String) {
        pixelfedAPI.instance().enqueue(object : Callback<Instance> {
                override fun onFailure(call: Call<Instance>, t: Throwable) {
                    return failedRegistration(getString(R.string.instance_error))
                }

                override fun onResponse(call: Call<Instance>, response: Response<Instance>) {
                    if (response.isSuccessful && response.body() != null) {
                        val instance = response.body() as Instance
                        storeInstance(db, instance)
                        storeUser(accessToken, instance.uri)
                    } else {
                        return failedRegistration(getString(R.string.instance_error))
                    }
                }
            })
    }

    private fun storeUser(accessToken: String, instance: String) {
        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.body() != null && response.isSuccessful) {
                        db.userDao().deActivateActiveUser()
                        val user = response.body() as Account
                        DBUtils.addUser(
                            db,
                            user,
                            instance,
                            activeUser = true,
                            accessToken = accessToken
                        )
                        db.close()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
                override fun onFailure(call: Call<Account>, t: Throwable) {
                }
            })
    }

}
