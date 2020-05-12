package com.h.pixeldroid

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.h.pixeldroid.utils.Utils
import kotlinx.android.synthetic.main.activity_login.connect_instance_button
import kotlinx.android.synthetic.main.activity_login.editText
import kotlinx.android.synthetic.main.activity_login.login_activity_connection_required_text
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_chooser
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_chooser_button
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_chooser_layout
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_chooser_offline_text
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_input_layout
import kotlinx.android.synthetic.main.activity_login.progressLayout
import kotlinx.android.synthetic.main.activity_login.whatsAnInstanceTextView
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
    private var chooserVisibility: Int = View.GONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingAnimation(true)
        appName = getString(R.string.app_name)
        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences("$PACKAGE_ID.pref", Context.MODE_PRIVATE)
        db = DBUtils.initDB(applicationContext)

        // check for stored accounts/instances
        val accounts: List<Map<String, String>> = getSavedAccounts()
        if (accounts.isNotEmpty()) {
            displayChooser(accounts)
            login_activity_instance_chooser_button.setOnClickListener {
                val choice: Int = login_activity_instance_chooser.selectedItemId.toInt()
                setPreferences(accounts[choice])
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        if (Utils.hasInternet(applicationContext)) {
            connect_instance_button.setOnClickListener {
                registerAppToServer(normalizeDomain(editText.text.toString()))
            }
            whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }
            inputVisibility = View.VISIBLE
        } else {
            if (accounts.isEmpty()) {
                login_activity_connection_required_text.visibility = View.VISIBLE
            } else {
                login_activity_instance_chooser_offline_text.visibility = View.VISIBLE
            }
        }
        loadingAnimation(false)
    }

    private fun getSavedAccounts(): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        val instances = db.instanceDao().getAll()
        for (user in db.userDao().getAll()) {
            val instance = instances.first {instance ->
                instance.uri == user.instance_uri
            }
            result.add(mapOf(
                Pair("username", user.username),
                Pair("instance_title", instance.title),
                Pair("instance_uri", instance.uri),
                Pair("id", user.user_id)
            ))
        }
        return result.toImmutableList()
    }

    private fun displayChooser(accounts: List<Map<String, String>>) {
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            accounts.map { account ->
                "${account["username"]}@${account["instance_title"]}"
            }).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            login_activity_instance_chooser.adapter = adapter
        }
        chooserVisibility = View.VISIBLE
    }

    private fun setPreferences(account: Map<String, String>) {
        if (Utils.hasInternet(applicationContext))
            registerAppToServer(normalizeDomain(account["instance_uri"].orEmpty()))
        else
            preferences.edit()
                .putString("user_id", account["id"])
//                .putString("domain", account["instance_uri"])
//                .putString("clientID", clientId)
//                .putString("clientSecret", credentials.client_secret)
//                .putString("accessToken", accessToken)
//                .putString("instance_uri", instance.uri)
                .apply()
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

    private fun normalizeDomain(domain: String): String =
        "https://" + domain
            .replace("http://", "")
            .replace("https://", "")
            .trim(Char::isWhitespace)

    private fun registerAppToServer(normalizedDomain: String) {
        loadingAnimation(true)
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

        if (code.isNullOrEmpty() || domain.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty()) {
            return failedRegistration(getString(R.string.auth_failed))
        }

        //Successful authorization
        val callback = object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.auth_error_toast_msg),
                        Toast.LENGTH_LONG
                    ).show()
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
        preferences.edit().putString("accessToken", accessToken).apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun failedRegistration(message: String = getString(R.string.registration_failed)){
        loadingAnimation(false)
        editText.error = message
    }

    private fun loadingAnimation(on: Boolean){
        if(on) {
            login_activity_instance_input_layout.visibility = View.GONE
            login_activity_instance_chooser_layout.visibility = View.GONE
            progressLayout.visibility = View.VISIBLE
        }
        else {
            login_activity_instance_input_layout.visibility = inputVisibility
            login_activity_instance_chooser_layout.visibility = chooserVisibility
            progressLayout.visibility = View.GONE
        }
    }

    private fun saveUserAndInstance(accessToken: String) {
        preferences.edit().putInt("max_toot_chars", Instance.DEFAULT_MAX_TOOT_CHARS).apply()
        pixelfedAPI.instance().enqueue(object : Callback<Instance> {
                override fun onFailure(call: Call<Instance>, t: Throwable) {
                    Log.e(TAG, "Request to fetch instance config failed.")
                }
                override fun onResponse(call: Call<Instance>, response: Response<Instance>) {
                    if (response.isSuccessful && response.body() != null) {
                        val instance = response.body() as Instance
                        storeInstance(instance)
                        storeUser(accessToken)
                    } else {
                        Log.e(TAG, "Server response to fetch instance config failed.")
                    }
                }
            })
    }

    private fun storeInstance(instance: Instance) {
        val maxTootChars = instance.max_toot_chars.toInt()
        preferences.edit().putInt("max_toot_chars", maxTootChars).apply()
        preferences.edit().putString("instance_uri", instance.uri).apply()
        val dbInstance = InstanceDatabaseEntity(
            uri = instance.uri,
            title = instance.title,
            max_toot_chars = maxTootChars,
            thumbnail = instance.thumbnail
        )
        db.instanceDao().insertInstance(dbInstance)
    }

    private fun storeUser(accessToken: String) {
        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.body() != null && response.isSuccessful) {
                        val user = response.body() as Account
                        preferences.edit().putString("user_id", user.id).apply()
                        DBUtils.addUser(
                            db,
                            user,
                            preferences.getString("instance_uri", null).orEmpty()
                        )
                    }
                }
                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e(TAG, t.toString())
                }
            })
    }

}
