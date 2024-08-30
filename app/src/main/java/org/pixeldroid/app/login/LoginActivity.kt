package org.pixeldroid.app.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.pixeldroid.app.BuildConfig
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityLoginBinding
import org.pixeldroid.app.main.MainActivity
import org.pixeldroid.app.settings.SettingsActivity
import org.pixeldroid.app.settings.TutorialSettingsDialog.Companion.START_TUTORIAL
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.openUrl

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

class LoginActivity : BaseActivity() {

    companion object {
        private const val PACKAGE_ID = BuildConfig.APPLICATION_ID
        private const val PREFERENCE_NAME = "$PACKAGE_ID.loginPref"
        private const val SCOPE = "read write follow"
    }

    private lateinit var oauthScheme: String
    private lateinit var preferences: SharedPreferences

    private lateinit var binding: ActivityLoginBinding
    val model: LoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oauthScheme = getString(R.string.auth_scheme)
        preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        binding.connectInstanceButton.setOnClickListener {
            hideKeyboard()
            model.registerAppToServer(binding.editText.text.toString())
        }
        binding.whatsAnInstanceTextView.setOnClickListener{ whatsAnInstance() }

        // Enter button on keyboard should press the connect button
        binding.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.connectInstanceButton.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.promptOauth.collectLatest {
                    it?.let {
                        if (it.launch) promptOAuth(it.normalizedDomain, it.clientId)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.finishedLogin.collectLatest {
                    when (it) {
                        LoginActivityViewModel.FinishedLogin.Finished -> {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        LoginActivityViewModel.FinishedLogin.FinishedFirstTime -> MaterialAlertDialogBuilder(binding.root.context)
                            .setMessage(R.string.first_time_question)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val intent = Intent(this@LoginActivity, SettingsActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra(START_TUTORIAL, true)
                                startActivity(intent)
                            }
                            .setNegativeButton(R.string.skip_tutorial) { _, _ -> model.finishLogin()}
                            .show()
                        LoginActivityViewModel.FinishedLogin.NotFinished -> {}
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.loadingState.collectLatest {
                    when(it.loginState){
                        LoginActivityViewModel.LoginState.LoadingState.Resting -> loadingAnimation(false)
                        LoginActivityViewModel.LoginState.LoadingState.Busy -> loadingAnimation(true)
                        LoginActivityViewModel.LoginState.LoadingState.Error -> failedRegistration(it.error!!)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val url: Uri? = intent.data

        //Check if the activity was started after the authentication
        if (url == null || !url.toString().startsWith("$oauthScheme://$PACKAGE_ID")) return

        val code = url.getQueryParameter("code")
        model.authenticate(code)
    }

    private fun whatsAnInstance() {
        MaterialAlertDialogBuilder(this)
            .setView(layoutInflater.inflate(R.layout.whats_an_instance_explanation, null))
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            // Create the AlertDialog
            .show()
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

    private fun promptOAuth(normalizedDomain: String, client_id: String) {
        val url = "$normalizedDomain/oauth/authorize?" +
                "client_id" + "=" + client_id + "&" +
                "redirect_uri" + "=" + "$oauthScheme://$PACKAGE_ID" + "&" +
                "response_type=code" + "&" +
                "scope=${SCOPE.replace(" ", "%20")}"

        if (!openUrl(url)) model.oauthLaunchFailed()
        else model.oauthLaunched()
    }

    private fun failedRegistration(@StringRes message: Int = R.string.registration_failed) {
        when (message) {
            R.string.instance_not_pixelfed_warning -> MaterialAlertDialogBuilder(this@LoginActivity).apply {
                setMessage(R.string.instance_not_pixelfed_warning)
                setPositiveButton(R.string.instance_not_pixelfed_continue) { _, _ ->
                    model.dialogAckedContinueAnyways()
                }
                setNegativeButton(R.string.instance_not_pixelfed_cancel) { _, _ ->
                    model.dialogNegativeButtonClicked()
                }
            }.show()

            R.string.api_not_enabled_dialog -> MaterialAlertDialogBuilder(this@LoginActivity).apply {
                setMessage(R.string.api_not_enabled_dialog)
                setNegativeButton(android.R.string.ok) { _, _ ->
                    model.dialogNegativeButtonClicked()
                }
            }.show()

            else -> binding.editText.error = getString(message)
        }
        loadingAnimation(false)
    }

    private fun loadingAnimation(on: Boolean){
        if(on) {
            binding.loginActivityInstanceInputLayout.visibility = View.GONE
            binding.progressLayout.visibility = View.VISIBLE
        }
        else {
            binding.loginActivityInstanceInputLayout.visibility = View.VISIBLE
            binding.progressLayout.visibility = View.GONE
        }
    }

}
