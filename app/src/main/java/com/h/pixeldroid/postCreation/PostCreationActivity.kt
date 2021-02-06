package com.h.pixeldroid.postCreation

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
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.h.pixeldroid.MainActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityPostCreationBinding
import com.h.pixeldroid.postCreation.camera.CameraActivity
import com.h.pixeldroid.postCreation.carousel.CarouselItem
import com.h.pixeldroid.postCreation.carousel.ImageCarousel
import com.h.pixeldroid.postCreation.photoEdit.PhotoEditActivity
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Attachment
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

private const val TAG = "Post Creation Activity"
private const val MORE_PICTURES_REQUEST_CODE = 0xffff

data class PhotoData(
        var imageUri: Uri,
        var uploadId: String? = null,
        var progress: Int? = null,
        var imageDescription: String? = null
)

class PostCreationActivity : BaseActivity() {

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI

    private var albumLimit by Delegates.notNull<Int>()

    private var positionResult = 0
    private var user: UserDatabaseEntity? = null

    private val photoData: ArrayList<PhotoData> = ArrayList()

    private lateinit var binding: ActivityPostCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = db.userDao().getActiveUser()

        val instance: InstanceDatabaseEntity = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        albumLimit = instance.albumLimit

        // get image URIs
        intent.clipData?.let { addPossibleImages(it) }

        accessToken = user?.accessToken.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        val carousel: ImageCarousel = binding.carousel
        carousel.addData(photoData.map { CarouselItem(it.imageUri) })
        carousel.layoutCarouselCallback = {
            if(it){
                // Became a carousel
                binding.toolbar3.visibility = VISIBLE
            } else {
                // Became a grid
                binding.toolbar3.visibility = INVISIBLE
            }
        }
        carousel.maxEntries = albumLimit
        carousel.addPhotoButtonCallback = {
            addPhoto(applicationContext)
        }
        carousel.updateDescriptionCallback = { position: Int, description: String ->
            photoData[position].imageDescription = description
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
            addPhoto(it.context)
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
        if(count + photoData.size > albumLimit){
            AlertDialog.Builder(this).apply {
                setMessage(getString(R.string.total_exceeds_album_limit).format(albumLimit))
                setNegativeButton(android.R.string.ok) { _, _ -> }
            }.show()
            count = count.coerceAtMost(albumLimit - photoData.size)
        }
        if (count + photoData.size >= albumLimit) {
            // Disable buttons to add more images
            binding.addPhotoButton.isEnabled = false
        }
        for (i in 0 until count) {
            clipData.getItemAt(i).uri.let {
                photoData.add(PhotoData(it))
            }
        }
    }

    private fun addPhoto(context: Context){
        val intent = Intent(context, CameraActivity::class.java)
        this@PostCreationActivity.startActivityForResult(intent, MORE_PICTURES_REQUEST_CODE)
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
            val imageInputStream = contentResolver.openInputStream(imageUri)!!

            val size =
                if (imageUri.toString().startsWith("content")) {
                    contentResolver.query(imageUri, null, null, null, null)
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
                    imageUri.toFile().length()
                }

            val imagePart = ProgressRequestBody(imageInputStream, size)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis().toString(), imagePart)
                .build()

            val sub = imagePart.progressSubject
                .subscribeOn(Schedulers.io())
                .subscribe { percentage ->
                    data.progress = percentage.toInt()
                    binding.uploadProgressBar.progress =
                        photoData.sumBy { it.progress ?: 0 } / photoData.size
                }

            var postSub: Disposable? = null

            val description = data.imageDescription?.let { MultipartBody.Part.createFormData("description", it) }


            val inter = pixelfedAPI.mediaUpload("Bearer $accessToken", description, requestBody.parts[0])

            postSub = inter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { attachment: Attachment ->
                        data.progress = 0
                        data.uploadId = attachment.id!!
                    },
                    { e ->
                        binding.uploadError.visibility = View.VISIBLE
                        e.printStackTrace()
                        postSub?.dispose()
                        sub.dispose()
                    },
                    {
                        data.progress = 100
                        if(photoData.all{it.progress == 100 && it.uploadId != null}){
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
                pixelfedAPI.postStatus(
                    authorization = "Bearer $accessToken",
                    statusText = description,
                    media_ids = photoData.mapNotNull { it.uploadId }.toList()
                )
                Toast.makeText(applicationContext,getString(R.string.upload_post_success),
                    Toast.LENGTH_SHORT).show()
                val intent = Intent(this@PostCreationActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (exception: IOException) {
                Toast.makeText(applicationContext,getString(R.string.upload_post_error),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.toString())
                enableButton(true)
            } catch (exception: HttpException) {
                Toast.makeText(applicationContext,getString(R.string.upload_post_failed),
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

    private fun edit(position: Int) {
        positionResult = position

        val intent = Intent(this, PhotoEditActivity::class.java)
            .putExtra("picture_uri", photoData[position].imageUri)
            .putExtra("no upload", false)
        startActivityForResult(intent, positionResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == positionResult) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                photoData[positionResult].imageUri = data.getStringExtra("result")!!.toUri()

                binding.carousel.addData(photoData.map { CarouselItem(it.imageUri, it.imageDescription) })

                photoData[positionResult].progress = null
                photoData[positionResult].uploadId = null
            } else if(resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == MORE_PICTURES_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_OK && data?.clipData != null) {
                data.clipData?.let {
                    addPossibleImages(it)
                }

                binding.carousel.addData(photoData.map { CarouselItem(it.imageUri, it.imageDescription) })
            } else if(resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Error while adding images", Toast.LENGTH_SHORT).show()
            }
        }
    }
}