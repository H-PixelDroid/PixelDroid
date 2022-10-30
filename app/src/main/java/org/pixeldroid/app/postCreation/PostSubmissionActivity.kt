package org.pixeldroid.app.postCreation

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.View.GONE
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.databinding.ActivityPostSubmissionBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.carousel.CarouselItem
import org.pixeldroid.app.utils.BaseThemedWithoutBarActivity
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.fileExtension
import org.pixeldroid.app.utils.getMimeType
import org.pixeldroid.app.utils.setSquareImageFromURL
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class PostSubmissionActivity : BaseThemedWithoutBarActivity() {

    companion object {
        internal const val PICTURE_DESCRIPTION = "picture_description"
        internal const val TEMP_FILES = "temp_files"
        internal const val POST_REDRAFT = "post_redraft"
        internal const val PHOTO_DATA = "photo_data"
    }

    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private lateinit var binding: ActivityPostSubmissionBinding

    private lateinit var model: PostSubmissionViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostSubmissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        instance = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        val photoData = intent.getParcelableArrayListExtra<PhotoData>(PHOTO_DATA) as ArrayList<PhotoData>?

        val _model: PostSubmissionViewModel by viewModels {
            PostSubmissionViewModelFactory(
                application,
                photoData!!,
                instance
            )
        }
        model = _model

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

                    binding.uploadErrorTextExplanation.text = uiState.uploadErrorExplanationText
                }
            }
        }
        binding.newPostDescriptionInputField.doAfterTextChanged {
            model.newPostDescriptionChanged(binding.newPostDescriptionInputField.text)
        }

        val existingDescription: String? = intent.getStringExtra(PICTURE_DESCRIPTION)

        binding.newPostDescriptionInputField.setText(
            // Set description from redraft if any, otherwise from the template
            existingDescription ?: model.uiState.value.newPostDescriptionText
        )


        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        val firstPostImage = photoData!![0]
        setSquareImageFromURL(View(applicationContext), firstPostImage.imageUri.toString(), binding.postPreview)
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

    override fun onBackPressed() {
        val redraft = intent.getBooleanExtra(POST_REDRAFT, false)
        if (redraft) {
            val builder = AlertDialog.Builder(binding.root.context)
            builder.apply {
                setMessage(R.string.redraft_dialog_cancel)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    super.onBackPressed()
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                show()
            }
        } else {
            super.onBackPressed()
        }
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