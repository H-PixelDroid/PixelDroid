package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Application
import org.pixeldroid.app.utils.api.objects.Instance
import org.pixeldroid.app.utils.api.objects.NodeInfo
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.addUser
import org.pixeldroid.app.utils.db.storeInstance
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.normalizeDomain
import org.pixeldroid.app.utils.notificationsWorker.makeChannelGroupId
import org.pixeldroid.app.utils.notificationsWorker.makeNotificationChannels
import org.pixeldroid.app.utils.validDomain
import javax.inject.Inject

@HiltViewModel
class LoginActivityViewModel @Inject constructor(
    val apiHolder: PixelfedAPIHolder,
    val db: AppDatabase,
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {
    companion object {
        private const val PACKAGE_ID = BuildConfig.APPLICATION_ID
        private const val PREFERENCE_NAME = "$PACKAGE_ID.loginPref"
        private const val SCOPE = "read write follow"
    }
    private val oauthScheme = applicationContext.getString(R.string.auth_scheme)

    private lateinit var pixelfedAPI: PixelfedAPI
    private val preferences: SharedPreferences = applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    private val _error: MutableStateFlow<Int?> = MutableStateFlow(null)
    val error = _error.asStateFlow()

    private val _state: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.busy)
    val state = _state.asStateFlow()

    enum class LoginState {
        resting, busy, error
    }

    private fun registerAppToServer(normalizedDomain: String) {

        if(!validDomain(normalizedDomain)) return failedRegistration(R.string.invalid_domain)

        _state.value = LoginState.busy

        pixelfedAPI = PixelfedAPI.createFromUrl(normalizedDomain)

        viewModelScope.launch {
            try {
                val credentialsDeferred: Deferred<Application?> = async {
                    try {
                        pixelfedAPI.registerApplication(
                            applicationContext.getString(R.string.app_name),
                            "$oauthScheme://$PACKAGE_ID", SCOPE, "https://pixeldroid.org"
                        )
                    } catch (exception: Exception) {
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
                }?.href ?: return@launch failedRegistration(R.string.instance_error)

                nodeInfoSchema(normalizedDomain, clientId, nodeInfoSchemaUrl)
            } catch (exception: Exception) {
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
        } catch (exception: Exception) {
            return@coroutineScope failedRegistration(R.string.instance_error)
        }
        val domain: String = try {
            if (nodeInfo.hasInstanceEndpointInfo()) {
                preferences.edit().putString("nodeInfo", Gson().toJson(nodeInfo)).remove("instance").apply()
                nodeInfo.metadata?.config?.site?.url
            } else {
                val instance: Instance = try {
                    pixelfedAPI.instance()
                } catch (exception: Exception) {
                    return@coroutineScope failedRegistration(R.string.instance_error)
                }
                preferences.edit().putString("instance", Gson().toJson(instance)).remove("nodeInfo").apply()
                instance.uri
            }
        } catch (e: IllegalArgumentException){ null }
            ?: return@coroutineScope failedRegistration(R.string.instance_error)

        preferences.edit()
            .putString("domain", normalizeDomain(domain))
            .apply()


        if (!nodeInfo.software?.name.orEmpty().contains("pixelfed")) {
            MaterialAlertDialogBuilder(this@LoginActivity).apply {
                setMessage(R.string.instance_not_pixelfed_warning)
                setPositiveButton(R.string.instance_not_pixelfed_continue) { _, _ ->
                    promptOAuth(normalizedDomain, clientId)
                }
                setNegativeButton(R.string.instance_not_pixelfed_cancel) { _, _ ->
                    _state.value = LoginState.resting
                    wipeSharedSettings()
                }
            }.show()
        } else if (nodeInfo.metadata?.config?.features?.mobile_apis != true) {
            MaterialAlertDialogBuilder(this@LoginActivity).apply {
                setMessage(R.string.api_not_enabled_dialog)
                setNegativeButton(android.R.string.ok) { _, _ ->
                    _state.value = LoginState.resting
                    wipeSharedSettings()
                }
            }.show()
        } else {
            promptOAuth(normalizedDomain, clientId)
        }
    }

    private fun authenticate(code: String?) {

        // Get previous values from preferences
        val domain = preferences.getString("domain", "") as String
        val clientId = preferences.getString("clientID", "") as String
        val clientSecret = preferences.getString("clientSecret", "") as String

        if (code.isNullOrBlank() || domain.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            return failedRegistration(R.string.auth_failed)
        }

        //Successful authorization
        pixelfedAPI = PixelfedAPI.createFromUrl(domain)
        val gson = Gson()
        val nodeInfo: NodeInfo? = gson.fromJson(preferences.getString("nodeInfo", null), NodeInfo::class.java)
        val instance: Instance? = gson.fromJson(preferences.getString("instance", null), Instance::class.java)

        viewModelScope.launch {
            try {
                val token = pixelfedAPI.obtainToken(
                    clientId, clientSecret, "$oauthScheme://${PACKAGE_ID}",
                    SCOPE, code,
                    "authorization_code"
                )
                if (token.access_token == null) {
                    return@launch failedRegistration(R.string.token_error)
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
            } catch (exception: Exception) {
                return@launch failedRegistration(R.string.token_error)
            }
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
        } catch (exception: Exception) {
            return failedRegistration(R.string.verify_credentials)
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
        } catch (exception: Exception) {
            return failedRegistration(R.string.login_notifications)
        }

        makeNotificationChannels(
            applicationContext,
            user.fullHandle,
            makeChannelGroupId(user)
        )
    }


    private fun wipeSharedSettings(){
        preferences.edit().clear().apply()
    }

    private fun failedRegistration(message: Int = R.string.registration_failed) {
        _state.value = LoginState.error
        _error.value = message //TODO binding.editText.error = message
        wipeSharedSettings()
    }

}