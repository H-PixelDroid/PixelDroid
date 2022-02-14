package org.pixeldroid.app.postCreation

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.carousel.CarouselItem
import org.pixeldroid.app.postCreation.carousel.ImageCarousel
import org.pixeldroid.app.postCreation.photoEdit.PhotoEditActivity
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

private const val TAG = "Post Creation Activity"

data class PhotoData(
        var imageUri: Uri,
        var size: Long,
        var uploadId: String? = null,
        var progress: Int? = null,
        var imageDescription: String? = null,
)

class PostCreationActivity : BaseActivity() {

    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private val photoData: ArrayList<PhotoData> = ArrayList()

    private lateinit var binding: ActivityPostCreationBinding

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

        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        // get image URIs
        intent.clipData?.let { addPossibleImages(it) }

        val carousel: ImageCarousel = binding.carousel
        carousel.addData(photoData.map { CarouselItem(it.imageUri) })
        carousel.layoutCarouselCallback = {
            if(it){
                // Became a carousel
                binding.toolbarPostCreation.visibility = VISIBLE
            } else {
                // Became a grid
                binding.toolbarPostCreation.visibility = INVISIBLE
            }
        }
        carousel.maxEntries = instance.albumLimit
        carousel.addPhotoButtonCallback = {
            addPhoto()
        }
        carousel.updateDescriptionCallback = { position: Int, description: String ->
            photoData.getOrNull(position)?.imageDescription = description
        }

        // get the description and send the post
        binding.postCreationSendButton.setOnClickListener {
            if (validateDescription() && photoData.isNotEmpty()) upload()
        }

        // Button to retry image upload when it fails
        binding.retryUploadButton.setOnClickListener {
            binding.uploadError.visibility = View.GONE
            photoData.forEach {
                it.uploadId = null
                it.progress = null
            }
            upload()
        }

        binding.editPhotoButton.setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                edit(currentPosition)
            }
        }

        binding.addPhotoButton.setOnClickListener {
            addPhoto()
        }

        binding.savePhotoButton.setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                savePicture(it, currentPosition)
            }
        }


        binding.removePhotoButton.setOnClickListener {
            carousel.currentPosition.takeIf { it != -1 }?.let { currentPosition ->
                photoData.removeAt(currentPosition)
                carousel.addData(photoData.map { CarouselItem(it.imageUri, it.imageDescription) })
                binding.addPhotoButton.isEnabled = true
            }
        }
    }

    /**
     * Will add as many images as possible to [photoData], from the [clipData], and if
     * ([photoData].size + [clipData].itemCount) > [albumLimit] then it will only add as many images
     * as are legal (if any) and a dialog will be shown to the user alerting them of this fact.
     */
    private fun addPossibleImages(clipData: ClipData){
        var count = clipData.itemCount
        if(count + photoData.size > instance.albumLimit){
            AlertDialog.Builder(this).apply {
                setMessage(getString(R.string.total_exceeds_album_limit).format(instance.albumLimit))
                setNegativeButton(android.R.string.ok) { _, _ -> }
            }.show()
            count = count.coerceAtMost(instance.albumLimit - photoData.size)
        }
        if (count + photoData.size >= instance.albumLimit) {
            // Disable buttons to add more images
            binding.addPhotoButton.isEnabled = false
        }
        for (i in 0 until count) {
            clipData.getItemAt(i).uri.let {
                val size = it.getSizeAndValidate()
                photoData.add(PhotoData(imageUri = it, size = size))
            }
        }
    }

    /**
     * Returns the size of the file of the Uri, and opens a dialog in case it is too big or in case
     * the file is unsupported.
     */
    private fun Uri.getSizeAndValidate(): Long {
        val size: Long =
                if (toString().startsWith("content")) {
                    contentResolver.query(this, null, null, null, null)
                            ?.use { cursor ->
                                /* Get the column indexes of the data in the Cursor,
                                                 * move to the first row in the Cursor, get the data,
                                                 * and display it.
                                                 */
                                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                                cursor.moveToFirst()
                                cursor.getLong(sizeIndex)
                            } ?: 0
                } else {
                    toFile().length()
                }

        val sizeInkBytes = ceil(size.toDouble() / 1000).toLong()
        val type = contentResolver.getType(this)
        val isVideo = type?.startsWith("video/") == true

        if(isVideo && !instance.videoEnabled){
            AlertDialog.Builder(this@PostCreationActivity).apply {
                setMessage(R.string.video_not_supported)
                setNegativeButton(android.R.string.ok) { _, _ -> }
            }.show()
        }

        if (sizeInkBytes > instance.maxPhotoSize || sizeInkBytes > instance.maxVideoSize) {
            val maxSize = if (isVideo) instance.maxVideoSize else instance.maxPhotoSize
            AlertDialog.Builder(this@PostCreationActivity).apply {
                setMessage(getString(R.string.size_exceeds_instance_limit, photoData.size + 1, sizeInkBytes, maxSize))
                setNegativeButton(android.R.string.ok) { _, _ -> }
            }.show()
        }
        return size
    }

    private val addPhotoResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.clipData != null) {
            result.data?.clipData?.let {
                addPossibleImages(it)
            }
            binding.carousel.addData(photoData.map { CarouselItem(it.imageUri, it.imageDescription) })
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(applicationContext, "Error while adding images", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPhoto(){
        addPhotoResultContract.launch(
                Intent(this, CameraActivity::class.java)
        )
    }

    private fun savePicture(button: View, currentPosition: Int) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".png"
        val pair = getOutputFile(name)
        val outputStream: OutputStream = pair.first
        val path: String = pair.second

        contentResolver.openInputStream(photoData[currentPosition].imageUri)!!.use { input ->
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

    private fun getOutputFile(name: String): Pair<OutputStream, String> {
        val outputStream: OutputStream
        val path: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
            )
            val imageUri: Uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
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


    private fun validateDescription(): Boolean {
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

    /**
     * Uploads the images that are in the [photoData] array.
     * Keeps track of them in the [PhotoData.progress] (for the upload progress), and the
     * [PhotoData.uploadId] (for the list of ids of the uploads).
     */
    private fun upload() {
        enableButton(false)
        binding.uploadProgressBar.visibility = View.VISIBLE
        binding.uploadCompletedTextview.visibility = View.INVISIBLE
        binding.removePhotoButton.isEnabled = false
        binding.editPhotoButton.isEnabled = false
        binding.addPhotoButton.isEnabled = false

        for (data: PhotoData in photoData) {
            val imageUri = data.imageUri
            val imageInputStream = try {
                contentResolver.openInputStream(imageUri)!!
            } catch (e: FileNotFoundException){
                AlertDialog.Builder(this).apply {
                    setMessage(getString(R.string.file_not_found).format(imageUri))

                    setNegativeButton(android.R.string.ok) { _, _ -> }
                }.show()
                return
            }

            val imagePart = ProgressRequestBody(imageInputStream, data.size)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis().toString(), imagePart)
                .build()

            val sub = imagePart.progressSubject
                .subscribeOn(Schedulers.io())
                .subscribe { percentage ->
                    data.progress = percentage.toInt()
                    binding.uploadProgressBar.progress =
                        photoData.sumOf { it.progress ?: 0 } / photoData.size
                }

            var postSub: Disposable? = null

            val description = data.imageDescription?.let { MultipartBody.Part.createFormData("description", it) }

            val api = apiHolder.api ?: apiHolder.setToCurrentUser()
            val inter = api.mediaUpload(description, requestBody.parts[0])

            postSub = inter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { attachment: Attachment ->
                            data.progress = 0
                            data.uploadId = attachment.id!!
                        },
                        { e: Throwable ->
                            binding.uploadError.visibility = View.VISIBLE
                            if(e is HttpException){
                                binding.uploadErrorTextExplanation.text =
                                        getString(R.string.upload_error, e.code())
                                binding.uploadErrorTextExplanation.visibility= VISIBLE
                            } else {
                                binding.uploadErrorTextExplanation.visibility= View.GONE
                            }
                            e.printStackTrace()
                            postSub?.dispose()
                            sub.dispose()
                        },
                        {
                            data.progress = 100
                            if (photoData.all { it.progress == 100 && it.uploadId != null }) {
                                binding.uploadProgressBar.visibility = View.GONE
                                binding.uploadCompletedTextview.visibility = View.VISIBLE
                                post()
                            }
                            postSub?.dispose()
                            sub.dispose()
                        }
                )
        }
    }

    private fun post() {
        val description = binding.newPostDescriptionInputField.text.toString()
        enableButton(false)
        lifecycleScope.launchWhenCreated {
            try {
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()

                api.postStatus(
                        statusText = description,
                        media_ids = photoData.mapNotNull { it.uploadId }.toList()
                )
                Toast.makeText(applicationContext, getString(R.string.upload_post_success),
                        Toast.LENGTH_SHORT).show()
                val intent = Intent(this@PostCreationActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (exception: IOException) {
                Toast.makeText(applicationContext, getString(R.string.upload_post_error),
                        Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.toString())
                enableButton(true)
            } catch (exception: HttpException) {
                Toast.makeText(applicationContext, getString(R.string.upload_post_failed),
                        Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.response().toString() + exception.message().toString())
                enableButton(true)
            }
        }
    }

    private fun enableButton(enable: Boolean = true){
        binding.postCreationSendButton.isEnabled = enable
        if(enable){
            binding.postingProgressBar.visibility = View.GONE
            binding.postCreationSendButton.visibility = View.VISIBLE
        } else {
            binding.postingProgressBar.visibility = View.VISIBLE
            binding.postCreationSendButton.visibility = View.GONE
        }

    }

    private val editResultContract: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
            val position: Int = result.data!!.getIntExtra(PhotoEditActivity.PICTURE_POSITION, 0)
            photoData.getOrNull(position)?.apply {
                imageUri = result.data!!.getStringExtra(PhotoEditActivity.PICTURE_URI)!!.toUri()
                size = imageUri.getSizeAndValidate()
                progress = null
                uploadId = null
            } ?: Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()

            binding.carousel.addData(photoData.map { CarouselItem(it.imageUri, it.imageDescription) })
        } else if(result?.resultCode != Activity.RESULT_CANCELED){
            Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun edit(position: Int) {
        val intent = Intent(this, PhotoEditActivity::class.java)
            .putExtra(PhotoEditActivity.PICTURE_URI, photoData[position].imageUri)
            .putExtra(PhotoEditActivity.PICTURE_POSITION, position)
        editResultContract.launch(intent)
    }
}