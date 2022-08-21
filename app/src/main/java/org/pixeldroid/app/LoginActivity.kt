package org.pixeldroid.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.ActivityLoginBinding
import org.pixeldroid.app.utils.*
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Application
import org.pixeldroid.app.utils.api.objects.Instance
import org.pixeldroid.app.utils.api.objects.NodeInfo
import org.pixeldroid.app.utils.db.addUser
import org.pixeldroid.app.utils.db.storeInstance
import org.pixeldroid.app.utils.notificationsWorker.makeChannelGroupId
import org.pixeldroid.app.utils.notificationsWorker.makeNotificationChannels
import retrofit2.HttpException
import java.io.IOException

/**
Overview of the flow of the login process: (boxes are requests done in parallel,
since they do not depend on each other)

 _________________________________
|[PixelfedAPI.registerApplication]|
|[PixelfedAPI.wellKnownNodeInfo]  |
 ̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅
+----> [PixelfedAPI.nodeInfoSchema] (and then [PixelfedAPI.instance] if needed)
+----> [promptOAuth]
+----> [PixelfedAPI.obtainToken]
+----> [PixelfedAPI.verifyCredentials]

 */

class LoginActivity : BaseThemedWithoutBarActivity() {

    companion object {
        private const val PACKAGE_ID = BuildConfig.APPLICATION_ID
        private const val PREFERENCE_NAME = "$PACKAGE_ID.loginPref"
        private const val SCOPE = "read write follow"
    }

    private lateinit var oauthScheme: String
    private lateinit var appName: String
    private lateinit var preferences: SharedPreferences

    private lateinit var pixelfedAPI: PixelfedAPI
    private var inputVisibility: Int = View.GONE

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingAnimation(true)
        appName = getString(R.string.app_name)
        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        if (hasInternet(applicationContext)) {
            binding.connectInstanceButton.setOnClickListener {
                registerAppToServer(normalizeDomain(binding.editText.text.toString()))
            }
            binding.whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }
            inputVisibility = View.VISIBLE
        } else {
            binding.loginActivityConnectionRequired.visibility = View.VISIBLE
            binding.loginActivityConnectionRequiredButton.setOnClickListener {
                finish()
                startActivity(intent)
            }
        }
        loadingAnimation(false)
    }

    override fun onStart() {
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
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setView(layoutInflater.inflate(R.layout.whats_an_instance_explanation, null))
            setPositiveButton(android.R.string.ok) { _, _ -> }
        }
        // Create the AlertDialog
        builder.show()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    private fun registerAppToServer(normalizedDomain: String) {

        if(!validDomain(normalizedDomain)) return failedRegistration(getString(R.string.invalid_domain))

        hideKeyboard()
        loadingAnimation(true)

        pixelfedAPI = PixelfedAPI.createFromUrl(normalizedDomain)

        lifecycleScope.launch {
            try {
                val credentialsDeferred: Deferred<Application?> = async {
                    try {
                        pixelfedAPI.registerApplication(
                                appName, "$oauthScheme://$PACKAGE_ID", SCOPE, "https://pixeldroid.org"
                        )
                    } catch (exception: IOException) {
                        return@async null
                    } catch (exception: HttpException) {
                        return@async null
                    }
                }

                val nodeInfoJRD = pixelfedAPI.wellKnownNodeInfo()

                val credentials = credentialsDeferred.await()

                val clientId = credentials?.client_id ?: return@launch failedRegistration()
                preferences.edit()
                    .putString("clientID", clientId)
                    .putString("clientSecret", credentials.client_secret)
                    .apply()


                // c.f. https://nodeinfo.diaspora.software/protocol.html for more info
                val nodeInfoSchemaUrl = nodeInfoJRD.links.firstOrNull {
                    it.rel == "http://nodeinfo.diaspora.software/ns/schema/2.0"
                }?.href ?: return@launch failedRegistration(getString(R.string.instance_error))

                nodeInfoSchema(normalizedDomain, clientId, nodeInfoSchemaUrl)
            } catch (exception: IOException) {
                return@launch failedRegistration()
            } catch (exception: HttpException) {
                return@launch failedRegistration()
            }
        }
    }

    private suspend fun nodeInfoSchema(
        normalizedDomain: String,
        clientId: String,
        nodeInfoSchemaUrl: String
    ) = coroutineScope {

        val nodeInfo: NodeInfo = try {
            pixelfedAPI.nodeInfoSchema(nodeInfoSchemaUrl)
        } catch (exception: IOException) {
            return@coroutineScope failedRegistration(getString(R.string.instance_error))
        } catch (exception: HttpException) {
            return@coroutineScope failedRegistration(getString(R.string.instance_error))
        }

        val domain: String = try {
            if (nodeInfo.hasInstanceEndpointInfo()) {
                preferences.edit().putString("nodeInfo", Gson().toJson(nodeInfo)).remove("instance").apply()
                nodeInfo.metadata?.config?.site?.url
            } else {
                val instance: Instance = try {
                    pixelfedAPI.instance()
                } catch (exception: IOException) {
                    return@coroutineScope failedRegistration(getString(R.string.instance_error))
                } catch (exception: HttpException) {
                    return@coroutineScope failedRegistration(getString(R.string.instance_error))
                }
                preferences.edit().putString("instance", Gson().toJson(instance)).remove("nodeInfo").apply()
                instance.uri
            }
        } catch (e: IllegalArgumentException){ null }
                ?: return@coroutineScope failedRegistration(getString(R.string.instance_error))

        preferences.edit()
            .putString("domain", normalizeDomain(domain))
            .apply()


        if (!nodeInfo.software?.name.orEmpty().contains("pixelfed")) {
            AlertDialog.Builder(this@LoginActivity).apply {
                setMessage(R.string.instance_not_pixelfed_warning)
                setPositiveButton(R.string.instance_not_pixelfed_continue) { _, _ ->
                    promptOAuth(normalizedDomain, clientId)
                }
                setNegativeButton(R.string.instance_not_pixelfed_cancel) { _, _ ->
                    loadingAnimation(false)
                    wipeSharedSettings()
                }
            }.show()
        } else if (nodeInfo.metadata?.config?.features?.mobile_apis != true) {
            AlertDialog.Builder(this@LoginActivity).apply {
                setMessage(R.string.api_not_enabled_dialog)
                setNegativeButton(android.R.string.ok) { _, _ ->
                    loadingAnimation(false)
                    wipeSharedSettings()
                }
            }.show()
        } else {
            promptOAuth(normalizedDomain, clientId)
        }
    }


    private fun promptOAuth(normalizedDomain: String, client_id: String) {

        val url = "$normalizedDomain/oauth/authorize?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "$oauthScheme://$PACKAGE_ID" + "&" +
                "response_type=code" + "&" +
                "scope=${SCOPE.replace(" ", "%20")}"

        if (!openUrl(url)) return failedRegistration(getString(R.string.browser_launch_failed))
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
        pixelfedAPI = PixelfedAPI.createFromUrl(domain)
        val nodeInfo: NodeInfo? = Gson().fromJson(preferences.getString("nodeInfo", null), NodeInfo::class.java)
        val instance: Instance? = Gson().fromJson(preferences.getString("instance", null), Instance::class.java)

        lifecycleScope.launch {
            try {
                val token = pixelfedAPI.obtainToken(
                    clientId, clientSecret, "$oauthScheme://$PACKAGE_ID", SCOPE, code,
                    "authorization_code"
                )
                if (token.access_token == null) {
                    return@launch failedRegistration(getString(R.string.token_error))
                }
                storeInstance(db, nodeInfo, instance)
                storeUser(
                    token.access_token,
                    token.refresh_token,
                    clientId,
                    clientSecret,
                    domain
                )
                wipeSharedSettings()
            } catch (exception: IOException) {
                return@launch failedRegistration(getString(R.string.token_error))

            } catch (exception: HttpException) {
                return@launch failedRegistration(getString(R.string.token_error))
            }
        }
    }

    private fun failedRegistration(message: String = getString(R.string.registration_failed)) {
        loadingAnimation(false)
        binding.editText.error = message
        wipeSharedSettings()
    }

    private fun wipeSharedSettings(){
        preferences.edit().clear().apply()
    }

    private fun loadingAnimation(on: Boolean){
        if(on) {
            binding.loginActivityInstanceInputLayout.visibility = View.GONE
            binding.progressLayout.visibility = View.VISIBLE
        }
        else {
            binding.loginActivityInstanceInputLayout.visibility = inputVisibility
            binding.progressLayout.visibility = View.GONE
        }
    }

    private suspend fun storeUser(accessToken: String, refreshToken: String?, clientId: String, clientSecret: String, instance: String) {
        try {
            val user = pixelfedAPI.verifyCredentials("Bearer $accessToken")
            db.userDao().deActivateActiveUsers()
            addUser(
                db,
                user,
                instance,
                activeUser = true,
                accessToken = accessToken,
                refreshToken = refreshToken,
                clientId = clientId,
                clientSecret = clientSecret
            )
            apiHolder.setToCurrentUser()
        } catch (exception: IOException) {
            return failedRegistration(getString(R.string.verify_credentials))
        } catch (exception: HttpException) {
            return failedRegistration(getString(R.string.verify_credentials))
        }

        fetchNotifications()
        val intent = Intent(this@LoginActivity,  MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // Fetch the latest notifications of this account, to avoid launching old notifications
    private suspend fun fetchNotifications() {
        val user = db.userDao().getActiveUser()!!
        try {
            val notifications = apiHolder.api!!.notifications()

            notifications.forEach{it.user_id = user.user_id; it.instance_uri = user.instance_uri}

            db.notificationDao().insertAll(notifications)
        } catch (exception: IOException) {
            return failedRegistration(getString(R.string.login_notifications))
        } catch (exception: HttpException) {
            return failedRegistration(getString(R.string.login_notifications))
        } catch (exception: NullPointerException) {
            return failedRegistration(getString(R.string.login_notifications))
        }

        makeNotificationChannels(
            applicationContext,
            user.fullHandle,
            makeChannelGroupId(user)
        )
    }

}
