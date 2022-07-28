package org.pixeldroid.app.postCreation

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.arthenica.ffmpegkit.FFmpegKit
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.postCreation.photoEdit.PhotoEditActivity
import org.pixeldroid.app.postCreation.photoEdit.VideoEditActivity
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.getMimeType
import retrofit2.HttpException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlin.math.ceil

// Models the UI state for the PostCreationActivity
data class PostCreationActivityUiState(
    val userMessage: String? = null,

    val addPhotoButtonEnabled: Boolean = true,
    val editPhotoButtonEnabled: Boolean = true,
    val removePhotoButtonEnabled: Boolean = true,
    val postCreationSendButtonEnabled: Boolean = true,

    val isCarousel: Boolean = true,

    val newPostDescriptionText: String = "",

    val uploadProgressBarVisible: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadCompletedTextviewVisible: Boolean = false,
    val uploadErrorVisible: Boolean = false,
    val uploadErrorExplanationText: String = "",
    val uploadErrorExplanationVisible: Boolean = false,

    val newEncodingJobPosition: Int? = null,
    val newEncodingJobMuted: Boolean? = null,
    val newEncodingJobVideoStart: Float? = null,
    val newEncodingJobVideoEnd: Float? = null,
)

class PostCreationViewModel(application: Application, clipdata: ClipData? = null, val instance: InstanceDatabaseEntity? = null) : AndroidViewModel(application) {
    private val photoData: MutableLiveData<MutableList<PhotoData>> by lazy {
        MutableLiveData<MutableList<PhotoData>>().also {
           it.value =  clipdata?.let { it1 -> addPossibleImages(it1, mutableListOf()) }
        }
    }

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
    }

    // Map photoData indexes to FFmpeg Session IDs
    private val sessionMap: MutableMap<Int, Long> = mutableMapOf()
    // Keep track of temporary files to delete them (avoids filling cache super fast with videos)
    private val tempFiles: java.util.ArrayList<File> = java.util.ArrayList()


    private val _uiState = MutableStateFlow(PostCreationActivityUiState())
    val uiState: StateFlow<PostCreationActivityUiState> = _uiState

    fun userMessageShown() {
        _uiState.update { currentUiState ->
            currentUiState.copy(userMessage = null)
        }
    }

    fun encodingStarted() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                newEncodingJobPosition = null,
                newEncodingJobMuted = null,
                newEncodingJobVideoStart = null,
                newEncodingJobVideoEnd = null,
            )
        }
    }

    fun getPhotoData(): LiveData<MutableList<PhotoData>> = photoData

    /**
     * Will add as many images as possible to [photoData], from the [clipData], and if
     * ([photoData].size + [clipData].itemCount) > [InstanceDatabaseEntity.albumLimit] then it will only add as many images
     * as are legal (if any) and a dialog will be shown to the user alerting them of this fact.
     */
    fun addPossibleImages(clipData: ClipData, previousList: MutableList<PhotoData>? = photoData.value): MutableList<PhotoData> {
        val dataToAdd: ArrayList<PhotoData> = arrayListOf()
        var count = clipData.itemCount
        if(count + (previousList?.size ?: 0) > instance!!.albumLimit){
            _uiState.update { currentUiState ->
                currentUiState.copy(userMessage = getApplication<PixelDroidApplication>().getString(R.string.total_exceeds_album_limit).format(instance.albumLimit))
            }
            count = count.coerceAtMost(instance.albumLimit - (previousList?.size ?: 0))
        }
        if (count + (previousList?.size ?: 0) >= instance.albumLimit) {
            // Disable buttons to add more images
            _uiState.update { currentUiState ->
                currentUiState.copy(addPhotoButtonEnabled = false)
            }
        }
        for (i in 0 until count) {
            clipData.getItemAt(i).uri.let {
                val sizeAndVideoPair: Pair<Long, Boolean> =
                    getSizeAndVideoValidate(it, (previousList?.size ?: 0) + dataToAdd.size + 1)
                dataToAdd.add(PhotoData(imageUri = it, size = sizeAndVideoPair.first, video = sizeAndVideoPair.second))
            }
        }
        return previousList?.plus(dataToAdd)?.toMutableList() ?: mutableListOf()
    }

    fun setImages(addPossibleImages: MutableList<PhotoData>) {
        photoData.value = addPossibleImages
    }

    /**
     * Returns the size of the file of the Uri, and whether it is a video,
     * and opens a dialog in case it is too big or in case the file is unsupported.
     */
    fun getSizeAndVideoValidate(uri: Uri, editPosition: Int): Pair<Long, Boolean> {
        val size: Long =
            if (uri.scheme =="content") {
                getApplication<PixelDroidApplication>().contentResolver.query(uri, null, null, null, null)
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
                uri.toFile().length()
            }

        val sizeInkBytes = ceil(size.toDouble() / 1000).toLong()
        val type = uri.getMimeType(getApplication<PixelDroidApplication>().contentResolver)
        val isVideo = type.startsWith("video/")

        if(isVideo && !instance!!.videoEnabled){
            _uiState.update { currentUiState ->
                currentUiState.copy(userMessage = getApplication<PixelDroidApplication>().getString(R.string.video_not_supported))
            }
        }

        if (sizeInkBytes > instance!!.maxPhotoSize || sizeInkBytes > instance.maxVideoSize) {
            val maxSize = if (isVideo) instance.maxVideoSize else instance.maxPhotoSize
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    userMessage = getApplication<PixelDroidApplication>().getString(R.string.size_exceeds_instance_limit, editPosition, sizeInkBytes, maxSize)
                )
            }
        }
        return Pair(size, isVideo)
    }

    fun isNotEmpty(): Boolean = photoData.value?.isNotEmpty() ?: false

    fun updateDescription(position: Int, description: String) {
        photoData.value?.getOrNull(position)?.imageDescription = description
        photoData.value = photoData.value
    }

    fun resetUploadStatus() {
        photoData.value = photoData.value?.map { it.copy(uploadId = null, progress = null) }?.toMutableList()
    }

    fun setVideoEncodeAtPosition(position: Int, progress: Int?) {
        photoData.value?.set(position, photoData.value!![position].copy(videoEncodeProgress = progress))
        photoData.value = photoData.value
    }

    fun setUriAtPosition(uri: Uri, position: Int) {
        photoData.value?.set(position, photoData.value!![position].copy(imageUri = uri))
        photoData.value = photoData.value
    }

    fun setSizeAtPosition(imageSize: Long, position: Int) {
        photoData.value?.set(position, photoData.value!![position].copy(size = imageSize))
        photoData.value = photoData.value
    }

    fun removeAt(currentPosition: Int) {
        photoData.value?.removeAt(currentPosition)
        _uiState.update {
            it.copy(
                addPhotoButtonEnabled = true
            )
        }
        photoData.value = photoData.value
    }

    /**
     * Uploads the images that are in the [photoData] array.
     * Keeps track of them in the [PhotoData.progress] (for the upload progress), and the
     * [PhotoData.uploadId] (for the list of ids of the uploads).
     */
    fun upload() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                postCreationSendButtonEnabled = false,
                addPhotoButtonEnabled = false,
                editPhotoButtonEnabled = false,
                removePhotoButtonEnabled = false,
                uploadCompletedTextviewVisible = false,
                uploadErrorVisible = false,
                uploadProgressBarVisible = true
            )
        }

        for (data: PhotoData in getPhotoData().value ?: emptyList()) {
            val imageUri = data.imageUri
            val imageInputStream = try {
                getApplication<PixelDroidApplication>().contentResolver.openInputStream(imageUri)!!
            } catch (e: FileNotFoundException){
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        userMessage = getApplication<PixelDroidApplication>().getString(R.string.file_not_found,
                            imageUri)
                    )
                }
                return
            }

            val type = imageUri.getMimeType(getApplication<PixelDroidApplication>().contentResolver)
            val imagePart = ProgressRequestBody(imageInputStream, data.size, type)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis().toString(), imagePart)
                .build()

            val sub = imagePart.progressSubject
                .subscribeOn(Schedulers.io())
                .subscribe { percentage ->
                    data.progress = percentage.toInt()
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            uploadProgress = getPhotoData().value!!.sumOf { it.progress ?: 0 } / getPhotoData().value!!.size
                        )
                    }
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
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                uploadErrorVisible = true,
                                uploadErrorExplanationText = if(e is HttpException){
                                    getApplication<PixelDroidApplication>().getString(R.string.upload_error, e.code())
                                } else "",
                                uploadErrorExplanationVisible = e is HttpException,
                            )
                        }
                        e.printStackTrace()
                        postSub?.dispose()
                        sub.dispose()
                    },
                    {
                        data.progress = 100
                        if (getPhotoData().value!!.all { it.progress == 100 && it.uploadId != null }) {
                            _uiState.update { currentUiState ->
                                currentUiState.copy(
                                    uploadProgressBarVisible = false,
                                    uploadCompletedTextviewVisible = true
                                )
                            }
                            post()
                        }
                        postSub?.dispose()
                        sub.dispose()
                    }
                )
        }
    }

    private fun post() {
        val description = uiState.value.newPostDescriptionText
        _uiState.update { currentUiState ->
            currentUiState.copy(
                postCreationSendButtonEnabled = false
            )
        }
        viewModelScope.launch {
            try {
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()

                api.postStatus(
                    statusText = description,
                    media_ids = getPhotoData().value!!.mapNotNull { it.uploadId }.toList()
                )
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_success),
                    Toast.LENGTH_SHORT).show()
                val intent = Intent(getApplication(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                //TODO make the activity launch this instead (and surrounding toasts too)L
                getApplication<PixelDroidApplication>().startActivity(intent)
            } catch (exception: IOException) {
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_error),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.toString())
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        postCreationSendButtonEnabled = true
                    )
                }
            } catch (exception: HttpException) {
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_failed),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.response().toString() + exception.message().toString())
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        postCreationSendButtonEnabled = true
                    )
                }
            }
        }
    }

    fun modifyAt(position: Int, data: Intent): Unit? {
       val result: PhotoData = photoData.value?.getOrNull(position)?.run {
            if (video) {
                val muted: Boolean = data.getBooleanExtra(VideoEditActivity.MUTED, false)
                val videoStart: Float? = data.getFloatExtra(VideoEditActivity.VIDEO_START, -1f).let {
                    if(it == -1f) null else it
                }
                val modified: Boolean = data.getBooleanExtra(VideoEditActivity.MODIFIED, false)
                val videoEnd: Float? = data.getFloatExtra(VideoEditActivity.VIDEO_END, -1f).let {
                    if(it == -1f) null else it
                }
                if(modified){
                    videoEncodeProgress = 0
                    sessionMap[position]?.let { FFmpegKit.cancel(it) }
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            newEncodingJobPosition = position,
                            newEncodingJobMuted = muted,
                            newEncodingJobVideoStart = videoStart,
                            newEncodingJobVideoEnd = videoEnd
                        )
                    }
                }
            } else {
                imageUri = data.getStringExtra(PhotoEditActivity.PICTURE_URI)!!.toUri()
                val (imageSize, imageVideo) = getSizeAndVideoValidate(imageUri, position)
                size = imageSize
                video = imageVideo
            }
            progress = null
            uploadId = null
            this
        } ?: return null
        result.let {
            photoData.value?.set(position, it)
            photoData.value = photoData.value
        }
        return Unit
    }

    fun newPostDescriptionChanged(text: Editable?) {
        _uiState.update { it.copy(newPostDescriptionText = text.toString()) }
    }

    fun trackTempFile(file: File) {
        tempFiles.add(file)
    }

    fun cancelEncode(currentPosition: Int) {
        sessionMap[currentPosition]?.let { FFmpegKit.cancel(it) }
    }

    override fun onCleared() {
        super.onCleared()
        FFmpegKit.cancel()
        tempFiles.forEach {
            it.delete()
        }

    }

    fun registerNewFFmpegSession(position: Int, sessionId: Long) {
        sessionMap[position] = sessionId
    }

    fun becameCarousel(became: Boolean) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                isCarousel = became
            )
        }
    }

}

class PostCreationViewModelFactory(val application: Application, val clipdata: ClipData, val instance: InstanceDatabaseEntity) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, ClipData::class.java, InstanceDatabaseEntity::class.java).newInstance(application, clipdata, instance)
    }
}