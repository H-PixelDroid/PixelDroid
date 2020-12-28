package com.h.pixeldroid

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.browser.customtabs.CustomTabsIntent
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.db.addUser
import com.h.pixeldroid.utils.db.storeInstance
import com.h.pixeldroid.utils.api.objects.*
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.hasInternet
import com.h.pixeldroid.utils.normalizeDomain
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
Overview of the flow of the login process: (boxes are requests done in parallel,
since they do not depend on each other)

 _________________________________
|[PixelfedAPI.registerApplication]|
|[PixelfedAPI.wellKnownNodeInfo]  |
 ̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅ +----> [PixelfedAPI.nodeInfoSchema]
                                                                    +----> [promptOAuth]
                                                                                       +---->____________________________
                                                                                            |[PixelfedAPI.instance]      |
                                                                                            |[PixelfedAPI.obtainToken]   |
                                                                                             ̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅̅ +----> [PixelfedAPI.verifyCredentials]

 */

class LoginActivity : BaseActivity() {

    companion object {
        private const val PACKAGE_ID = BuildConfig.APPLICATION_ID
        private const val SCOPE = "read write follow"
    }

    private lateinit var oauthScheme: String
    private lateinit var appName: String
    private lateinit var preferences: SharedPreferences

    private lateinit var pixelfedAPI: PixelfedAPI
    private var inputVisibility: Int = View.GONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingAnimation(true)
        appName = getString(R.string.app_name)
        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences("$PACKAGE_ID.pref", Context.MODE_PRIVATE)

        if (hasInternet(applicationContext)) {
            connect_instance_button.setOnClickListener {
                registerAppToServer(normalizeDomain(editText.text.toString()))
            }
            whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }
            inputVisibility = View.VISIBLE
        } else {
            login_activity_connection_required.visibility = View.VISIBLE
            login_activity_connection_required_button.setOnClickListener {
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
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://pixelfed.org/join")
        startActivity(i)
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

        try{
            HttpUrl.Builder().host(normalizedDomain.replace("https://", "")).scheme("https").build()
        } catch (e: IllegalArgumentException) {
            return failedRegistration(getString(R.string.invalid_domain))
        }

        hideKeyboard()
        loadingAnimation(true)

        pixelfedAPI = PixelfedAPI.createFromUrl(normalizedDomain)
        
        Single.zip(
            pixelfedAPI.registerApplication(
                appName,"$oauthScheme://$PACKAGE_ID", SCOPE
            ),
            pixelfedAPI.wellKnownNodeInfo(),
            BiFunction<Application, NodeInfoJRD, Pair<Application, NodeInfoJRD>> { application, nodeInfoJRD ->
                // we get here when both results have come in:
                Pair(application, nodeInfoJRD)
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Pair<Application, NodeInfoJRD>> {
                override fun onSuccess(pair: Pair<Application, NodeInfoJRD>) {
                    val (credentials, nodeInfoJRD) = pair
                    val clientId = credentials.client_id ?: return failedRegistration()
                    preferences.edit()
                        .putString("domain", normalizedDomain)
                        .putString("clientID", clientId)
                        .putString("clientSecret", credentials.client_secret)
                        .apply()

                    //c.f. https://nodeinfo.diaspora.software/protocol.html for more info
                    val nodeInfoSchemaUrl = nodeInfoJRD.links.firstOrNull {
                            it.rel == "http://nodeinfo.diaspora.software/ns/schema/2.0"
                        }?.href ?: return failedRegistration(getString(R.string.instance_error))

                    nodeInfoSchema(normalizedDomain, clientId, nodeInfoSchemaUrl)
                }

                override fun onError(e: Throwable) {
                    //Error in any of the two requests will get to this
                    Log.e("registerAppToServer", e.message.toString())
                    failedRegistration()
                }

                override fun onSubscribe(d: Disposable) {}
            })
    }

    private fun nodeInfoSchema(
        normalizedDomain: String,
        clientId: String,
        nodeInfoSchemaUrl: String
    ) {
        pixelfedAPI.nodeInfoSchema(nodeInfoSchemaUrl).enqueue(object : Callback<NodeInfo> {
            override fun onResponse(call: Call<NodeInfo>, response: Response<NodeInfo>) {
                if (response.body() == null || !response.isSuccessful) {
                    return failedRegistration(getString(R.string.instance_error))
                }
                val nodeInfo = response.body() as NodeInfo

                if (!nodeInfo.software?.name.orEmpty().contains("pixelfed")) {
                    val builder = AlertDialog.Builder(this@LoginActivity)
                    builder.apply {
                        setMessage(R.string.instance_not_pixelfed_warning)
                        setPositiveButton(R.string.instance_not_pixelfed_continue) { _, _ ->
                            promptOAuth(normalizedDomain, clientId)
                        }
                        setNegativeButton(R.string.instance_not_pixelfed_cancel) { _, _ ->
                            loadingAnimation(false)
                            wipeSharedSettings()
                        }
                    }
                    // Create the AlertDialog
                    builder.show()
                    return
                } else {
                    promptOAuth(normalizedDomain, clientId)
                }
            }
            override fun onFailure(call: Call<NodeInfo>, t: Throwable) {
                failedRegistration(getString(R.string.instance_error))
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
            try {
                startActivity(browserIntent)
            } catch(e: ActivityNotFoundException) {
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
        pixelfedAPI = PixelfedAPI.createFromUrl(domain)

        //TODO check why we can't do onErrorReturn { null } which would make more sense ¯\_(ツ)_/¯
        //Also, maybe find a nicer way to do this, this feels hacky (although it can work fine)
        val nullInstance = Instance(null, null, null, null, null, null, null, null)
        val nullToken = Token(null, null, null, null, null)

        Single.zip(
            pixelfedAPI.instance().onErrorReturn { nullInstance },
            pixelfedAPI.obtainToken(
                clientId, clientSecret, "$oauthScheme://$PACKAGE_ID", SCOPE, code,
                "authorization_code"
            ).onErrorReturn { nullToken },
            BiFunction<Instance, Token, Pair<Instance, Token>> { instance, token ->
                // we get here when all results have come in:
                Pair(instance, token)
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Pair<Instance, Token>> {
                override fun onSuccess(triple: Pair<Instance, Token>) {
                    val (instance, token) = triple
                    if(token == nullToken || token.access_token == null){
                        return failedRegistration(getString(R.string.token_error))
                    } else if(instance == nullInstance || instance.uri == null){
                        return failedRegistration(getString(R.string.instance_error))
                    }

                    storeInstance(db, instance)
                    storeUser(token.access_token, token.refresh_token, clientId, clientSecret, instance.uri)
                    wipeSharedSettings()
                }

                override fun onError(e: Throwable) {
                    Log.e("saveUserAndInstance", e.message.toString())
                    failedRegistration(getString(R.string.token_error))
                }
                override fun onSubscribe(d: Disposable) {}
            })
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

    private fun storeUser(accessToken: String, refreshToken: String?, clientId: String, clientSecret: String, instance: String) {
        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.body() != null && response.isSuccessful) {
                        db.userDao().deActivateActiveUsers()
                        val user = response.body() as Account
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
                        apiHolder.setDomainToCurrentUser(db)
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
