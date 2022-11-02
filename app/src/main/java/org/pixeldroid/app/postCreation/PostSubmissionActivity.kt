package org.pixeldroid.app.postCreation

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostSubmissionBinding
import org.pixeldroid.app.postCreation.PostCreationActivity.Companion.TEMP_FILES
import org.pixeldroid.app.utils.BaseThemedWithoutBarActivity
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.setSquareImageFromURL
import java.io.File


class PostSubmissionActivity : BaseThemedWithoutBarActivity() {

    companion object {
        internal const val PICTURE_DESCRIPTION = "picture_description"
        internal const val PHOTO_DATA = "photo_data"
    }

    private lateinit var accounts: List<UserDatabaseEntity>
    private var selectedAccount: Int = -1
    private lateinit var menu: Menu
    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private lateinit var binding: ActivityPostSubmissionBinding

    private lateinit var model: PostSubmissionViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.add_details)

        user = db.userDao().getActiveUser()
        accounts = db.userDao().getAll()

        instance = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        val photoData = intent.getParcelableArrayListExtra<PhotoData>(PHOTO_DATA) as ArrayList<PhotoData>?

        val _model: PostSubmissionViewModel by viewModels {
            PostSubmissionViewModelFactory(
                application,
                photoData!!
            )
        }
        model = _model

        model.setExistingDescription(intent.getStringExtra(PICTURE_DESCRIPTION))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    uiState.userMessage?.let {
                        AlertDialog.Builder(binding.root.context).apply {
                            setMessage(it)
                            setNegativeButton(android.R.string.ok) { _, _ -> }
                        }.show()

                        // Notify the ViewModel the message is displayed
                        model.userMessageShown()
                    }
                    enableButton(uiState.postCreationSendButtonEnabled)
                    binding.uploadProgressBar.visibility =
                        if (uiState.uploadProgressBarVisible) VISIBLE else INVISIBLE
                    binding.uploadProgressBar.progress = uiState.uploadProgress
                    binding.uploadCompletedTextview.visibility =
                        if (uiState.uploadCompletedTextviewVisible) VISIBLE else INVISIBLE
                    binding.uploadError.visibility =
                        if (uiState.uploadErrorVisible) VISIBLE else INVISIBLE
                    binding.uploadErrorTextExplanation.visibility =
                        if (uiState.uploadErrorExplanationVisible) VISIBLE else INVISIBLE

                    selectedAccount = accounts.indexOf(uiState.chosenAccount)

                    binding.uploadErrorTextExplanation.text = uiState.uploadErrorExplanationText
                }
            }
        }
        binding.newPostDescriptionInputField.doAfterTextChanged {
            model.newPostDescriptionChanged(binding.newPostDescriptionInputField.text)
        }

        binding.nsfwSwitch.setOnCheckedChangeListener { _, isChecked ->
            model.updateNSFW(isChecked)
        }

        val existingDescription: String? = intent.getStringExtra(PICTURE_DESCRIPTION)

        binding.newPostDescriptionInputField.setText(
            // Set description from redraft if any, otherwise from the template
            existingDescription ?: model.uiState.value.newPostDescriptionText
        )


        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        setSquareImageFromURL(View(applicationContext), photoData!![0].imageUri.toString(), binding.postPreview)
        // get the description and send the post
        binding.postCreationSendButton.setOnClickListener {
            if (validatePost()) model.upload()
        }

        // Button to retry image upload when it fails
        binding.retryUploadButton.setOnClickListener {
            model.resetUploadStatus()
            model.upload()
        }

        // Clean up temporary files, if any
        val tempFiles = intent.getStringArrayExtra(TEMP_FILES)
        tempFiles?.asList()?.forEach {
            val file = File(binding.root.context.cacheDir, it)
            model.trackTempFile(file)
        }
    }

    override fun onCreateOptionsMenu(newMenu: Menu): Boolean {
        menuInflater.inflate(R.menu.post_submission_account_menu, newMenu)
        menu = newMenu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.action_switch_accounts -> {
                AlertDialog.Builder(this).apply {
                    setIcon(R.drawable.material_drawer_ico_account)
                    setTitle(R.string.switch_accounts)
                    setSingleChoiceItems(accounts.map { it.username + " (${it.fullHandle})" }.toTypedArray(), selectedAccount) { dialog, which ->
                        if(selectedAccount != which){
                            model.chooseAccount(accounts[which])
                        }
                        dialog.dismiss()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validatePost(): Boolean {
        binding.postTextInputLayout.run {
            val content = editText?.length() ?: 0
            if (content > counterMaxLength) {
                // error, too many characters
                error = resources.getQuantityString(R.plurals.description_max_characters, counterMaxLength, counterMaxLength)
                return false
            }
        }
        return true
    }

    private fun enableButton(enable: Boolean = true){
        binding.postCreationSendButton.isEnabled = enable
        if(enable){
            binding.postingProgressBar.visibility = GONE
            binding.postCreationSendButton.visibility = VISIBLE
        } else {
            binding.postingProgressBar.visibility = VISIBLE
            binding.postCreationSendButton.visibility = GONE
        }

    }
}