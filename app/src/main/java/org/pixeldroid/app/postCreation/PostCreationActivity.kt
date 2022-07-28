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
import androidx.core.os.HandlerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.carousel.CarouselItem
import org.pixeldroid.app.postCreation.photoEdit.PhotoEditActivity
import org.pixeldroid.app.postCreation.photoEdit.VideoEditActivity
import org.pixeldroid.app.utils.BaseThemedWithoutBarActivity
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.ffmpegSafeUri
import org.pixeldroid.app.utils.fileExtension
import org.pixeldroid.app.utils.getMimeType
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


const val TAG = "Post Creation Activity"

data class PhotoData(
    var imageUri: Uri,
    var size: Long,
    var uploadId: String? = null,
    var progress: Int? = null,
    var imageDescription: String? = null,
    var video: Boolean,
    var videoEncodeProgress: Int? = null,
)

class PostCreationActivity : BaseThemedWithoutBarActivity() {

    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private lateinit var binding: ActivityPostCreationBinding

    private val resultHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

    private lateinit var model: PostCreationViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = db.userDao().getActiveUser()

        instance = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        val _model: PostCreationViewModel by viewModels { PostCreationViewModelFactory(application, intent.clipData!!, instance) }
        model = _model

        model.getPhotoData().observe(this) { newPhotoData ->
            // update UI
            binding.carousel.addData(
                newPhotoData.map {
                    CarouselItem(it.imageUri, it.imageDescription, it.video, it.videoEncodeProgress)
                }
            )
        }

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
                    binding.addPhotoButton.isEnabled = uiState.addPhotoButtonEnabled
                    enableButton(uiState.postCreationSendButtonEnabled)
                    binding.uploadProgressBar.visibility = if(uiState.uploadProgressBarVisible) VISIBLE else INVISIBLE
                    binding.uploadProgressBar.progress = uiState.uploadProgress
                    binding.uploadCompletedTextview.visibility = if(uiState.uploadCompletedTextviewVisible) VISIBLE else INVISIBLE
                    binding.removePhotoButton.isEnabled = uiState.removePhotoButtonEnabled
                    binding.editPhotoButton.isEnabled = uiState.editPhotoButtonEnabled
                    binding.uploadError.visibility = if(uiState.uploadErrorVisible) VISIBLE else INVISIBLE
                    binding.uploadErrorTextExplanation.visibility = if(uiState.uploadErrorExplanationVisible) VISIBLE else INVISIBLE

                    binding.toolbarPostCreation.visibility = if(uiState.isCarousel) VISIBLE else INVISIBLE
                    binding.carousel.layoutCarousel = uiState.isCarousel


                    binding.uploadErrorTextExplanation.text = uiState.uploadErrorExplanationText

                    uiState.newEncodingJobPosition?.let { position ->
                        uiState.newEncodingJobMuted?.let { muted ->
                            uiState.newEncodingJobVideoStart?.let { videoStart ->
                                uiState.newEncodingJobVideoEnd?.let { videoEnd ->
                                    startEncoding(position, muted, videoStart, videoEnd)
                                    model.encodingStarted()
                                }
                            }
                        }
                    }
                }
            }
        }
        binding.newPostDescriptionInputField.doAfterTextChanged {
            model.newPostDescriptionChanged(binding.newPostDescriptionInputField.text)
        }
        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        binding.carousel.apply {
            layoutCarouselCallback = { model.becameCarousel(it)}
            maxEntries = instance.albumLimit
            addPhotoButtonCallback = {
                addPhoto()
            }
            updateDescriptionCallback = { position: Int, description: String ->
                model.updateDescription(position, description)
            }
        }
        // get the description and send the post
        binding.postCreationSendButton.setOnClickListener {
            if (validatePost() && model.isNotEmpty()) model.upload()
        }

        // Button to retry image upload when it fails
        binding.retryUploadButton.setOnClickListener {
            model.resetUploadStatus()
            model.upload()
        }

        binding.editPhotoButton.setOnClickListener {
            binding.carousel.currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { currentPosition ->
                edit(currentPosition)
            }
        }

        binding.addPhotoButton.setOnClickListener {
            addPhoto()
        }

        binding.savePhotoButton.setOnClickListener {
            binding.carousel.currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { currentPosition ->
                savePicture(it, currentPosition)
            }
        }


        binding.removePhotoButton.setOnClickListener {
            binding.carousel.currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { currentPosition ->
                model.removeAt(currentPosition)
                model.cancelEncode(currentPosition)
            }
        }
    }

    private val addPhotoResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.clipData != null) {
            result.data?.clipData?.let {
                model.setImages(model.addPossibleImages(it))
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(applicationContext, R.string.add_images_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPhoto(){
        addPhotoResultContract.launch(
                Intent(this, CameraActivity::class.java)
        )
    }

    private fun savePicture(button: View, currentPosition: Int) {
        val originalUri = model.getPhotoData().value!![currentPosition].imageUri

        val pair = getOutputFile(originalUri)
        val outputStream: OutputStream = pair.first
        val path: String = pair.second

        contentResolver.openInputStream(originalUri)!!.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        if(path.startsWith("file")) {
            MediaScannerConnection.scanFile(
                    this,
                    arrayOf(path.toUri().toFile().absolutePath),
                    null
            ) { path, uri ->
                if (uri == null) {
                    Log.e(
                            "NEW IMAGE SCAN FAILED",
                            "Tried to scan $path, but it failed"
                    )
                }
            }
        }
        Snackbar.make(
                button, getString(R.string.save_image_success),
                Snackbar.LENGTH_LONG
        ).show()
    }

    private fun getOutputFile(uri: Uri): Pair<OutputStream, String> {
        val extension = uri.fileExtension(contentResolver)

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".$extension"

        val outputStream: OutputStream
        val path: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val type = uri.getMimeType(contentResolver)
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, type)
            contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
            )
            val store =
                if (type.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val imageUri: Uri = resolver.insert(store, contentValues)!!
            path = imageUri.toString()
            outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri))!!
        } else {
            @Suppress("DEPRECATION") val imagesDir =
                Environment.getExternalStoragePublicDirectory(getString(R.string.app_name))
            imagesDir.mkdir()
            val file = File(imagesDir, name)
            path = Uri.fromFile(file).toString()
            outputStream = file.outputStream()
        }
        return Pair(outputStream, path)
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
        if(model.getPhotoData().value?.all { it.videoEncodeProgress == null } == false){
            AlertDialog.Builder(this).apply {
                setMessage(R.string.still_encoding)
                setNegativeButton(android.R.string.ok) { _, _ -> }
            }.show()
            return false
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

    private val editResultContract: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
            val position: Int = result.data!!.getIntExtra(PhotoEditActivity.PICTURE_POSITION, 0)
            model.modifyAt(position, result.data!!)
                ?: Toast.makeText(applicationContext, R.string.error_editing, Toast.LENGTH_SHORT).show()
        } else if(result?.resultCode != Activity.RESULT_CANCELED){
            Toast.makeText(applicationContext, R.string.error_editing, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * @param muted should audio tracks be removed in the output
     * @param videoStart when we want to start the video, in seconds, or null if we
     * don't want to remove the start
     * @param videoEnd when we want to end the video, in seconds, or null if we
     * don't want to remove the end
     */
    private fun startEncoding(position: Int, muted: Boolean, videoStart: Float?, videoEnd: Float?) {
        val originalUri = model.getPhotoData().value!![position].imageUri

        // Having a meaningful suffix is necessary so that ffmpeg knows what to put in output
        val suffix = originalUri.fileExtension(contentResolver)
        val file = File.createTempFile("temp_video", ".$suffix")
        //val file = File.createTempFile("temp_video", ".webm")
        model.trackTempFile(file)
        val fileUri = file.toUri()
        val outputVideoPath = ffmpegSafeUri(fileUri)

        val inputUri = model.getPhotoData().value!![position].imageUri

        val inputSafePath = ffmpegSafeUri(inputUri)

        val mediaInformation: MediaInformation? = FFprobeKit.getMediaInformation(ffmpegSafeUri(inputUri)).mediaInformation
        val totalVideoDuration = mediaInformation?.duration?.toFloatOrNull()

        val mutedString = if(muted) "-an" else ""
        val startString = if(videoStart != null) "-ss $videoStart" else ""

        val endString = if(videoEnd != null) "-to ${videoEnd - (videoStart ?: 0f)}" else ""

        val session: FFmpegSession = FFmpegKit.executeAsync("$startString -i $inputSafePath $endString -c copy $mutedString -y $outputVideoPath",
        //val session: FFmpegSession = FFmpegKit.executeAsync("$startString -i $inputSafePath $endString -c:v libvpx-vp9 -c:a copy -an -y $outputVideoPath",
            { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    fun successResult() {
                        // Hide progress indicator in carousel
                        binding.carousel.updateProgress(null, position, false)
                        val (imageSize, _) = outputVideoPath.toUri().let {
                            model.setUriAtPosition(it, position)
                            model.getSizeAndVideoValidate(it, position)
                        }
                        model.setVideoEncodeAtPosition(position, null)
                        model.setSizeAtPosition(imageSize, position)
                    }

                    val post = resultHandler.post {
                        successResult()
                    }
                    if(!post) {
                        Log.e(TAG, "Failed to post changes, trying to recover in 100ms")
                        resultHandler.postDelayed({successResult()}, 100)
                    }
                    Log.d(TAG, "Encode completed successfully in ${session.duration} milliseconds")
                } else {
                    resultHandler.post {
                        binding.carousel.updateProgress(null, position, error = true)
                        model.setVideoEncodeAtPosition(position, null)
                    }
                    Log.e(TAG, "Encode failed with state ${session.state} and rc $returnCode.${session.failStackTrace}")
                }
            },
            { log -> Log.d("PostCreationActivityEncoding", log.message) }
        ) { statistics: Statistics? ->

            val timeInMilliseconds: Int? = statistics?.time
            timeInMilliseconds?.let {
                if (timeInMilliseconds > 0) {
                    val completePercentage = totalVideoDuration?.let {
                        val newTotalDuration = it - (videoStart ?: 0f) - (it - (videoEnd ?: it))
                        timeInMilliseconds / (10*newTotalDuration)
                    }
                    resultHandler.post {
                        completePercentage?.let {
                            val rounded: Int = it.roundToInt()
                            model.setVideoEncodeAtPosition(position, rounded)
                            binding.carousel.updateProgress(rounded, position, false)
                        }
                    }
                    Log.d(TAG, "Encoding video: %$completePercentage.")
                }
            }
        }
        model.registerNewFFmpegSession(position, session.sessionId)
    }

    private fun edit(position: Int) {
        val intent = Intent(
            this,
            if(model.getPhotoData().value!![position].video) VideoEditActivity::class.java else PhotoEditActivity::class.java
        )
            .putExtra(PhotoEditActivity.PICTURE_URI, model.getPhotoData().value!![position].imageUri)
            .putExtra(PhotoEditActivity.PICTURE_POSITION, position)

        editResultContract.launch(intent)

    }
}