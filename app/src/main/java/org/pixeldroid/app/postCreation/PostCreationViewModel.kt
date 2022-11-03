package org.pixeldroid.app.postCreation

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.fromHtml
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.getMimeType
import org.pixeldroid.media_editor.photoEdit.VideoEditActivity
import java.io.File
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.arrayListOf
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.getOrNull
import kotlin.collections.indexOfFirst
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toMutableList
import kotlin.math.ceil


// Models the UI state for the PostCreationActivity
data class PostCreationActivityUiState(
    val userMessage: String? = null,

    val addPhotoButtonEnabled: Boolean = true,
    val editPhotoButtonEnabled: Boolean = true,
    val removePhotoButtonEnabled: Boolean = true,

    val isCarousel: Boolean = true,

    val newPostDescriptionText: String = "",
)

@Parcelize
data class PhotoData(
    var imageUri: Uri,
    var size: Long,
    var uploadId: String? = null,
    var progress: Int? = null,
    var imageDescription: String? = null,
    var video: Boolean,
    var videoEncodeProgress: Int? = null,
    var videoEncodeStabilizationFirstPass: Boolean? = null,
    var videoEncodeComplete: Boolean = false,
    var videoEncodeError: Boolean = false,
) : Parcelable

class PostCreationViewModel(application: Application, clipdata: ClipData? = null, val instance: InstanceDatabaseEntity? = null) : AndroidViewModel(application) {
    private val photoData: MutableLiveData<MutableList<PhotoData>> by lazy {
        MutableLiveData<MutableList<PhotoData>>().also {
            it.value =  clipdata?.let { it1 -> addPossibleImages(it1, mutableListOf()) }
        }
    }
    private var existingDescription: String? = null
    private var existingNSFW: Boolean = false

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private val _uiState: MutableStateFlow<PostCreationActivityUiState>

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application)
        val initialDescription = sharedPreferences.getString("prefill_description", "") ?: ""

        _uiState = MutableStateFlow(PostCreationActivityUiState(newPostDescriptionText = existingDescription ?: initialDescription))
    }

    val uiState: StateFlow<PostCreationActivityUiState> = _uiState

    // Map photoData indexes to FFmpeg Session IDs
    private val sessionMap: MutableMap<Uri, Long> = mutableMapOf()
    // Keep track of temporary files to delete them (avoids filling cache super fast with videos)
    private val tempFiles: java.util.ArrayList<File> = java.util.ArrayList()

    fun userMessageShown() {
        _uiState.update { currentUiState ->
            currentUiState.copy(userMessage = null)
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
            clipData.getItemAt(i).let {
                val sizeAndVideoPair: Pair<Long, Boolean> =
                    getSizeAndVideoValidate(it.uri, (previousList?.size ?: 0) + dataToAdd.size + 1)
                dataToAdd.add(PhotoData(imageUri = it.uri, size = sizeAndVideoPair.first, video = sizeAndVideoPair.second, imageDescription = it.text?.toString()))
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

    fun setExistingDescription(description: String?) {
        existingDescription = description
    }

    fun setExistingNSFW(sensitive: Boolean) {
        existingNSFW = sensitive
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
     * Next step
     */
    fun nextStep(context: Context) {
        val intent = Intent(context, PostSubmissionActivity::class.java)
        intent.putExtra(PostSubmissionActivity.PHOTO_DATA, getPhotoData().value?.let { ArrayList(it) })
        intent.putExtra(PostSubmissionActivity.PICTURE_DESCRIPTION, existingDescription)
        intent.putExtra(PostSubmissionActivity.POST_NSFW, existingNSFW)
        ContextCompat.startActivity(context, intent, null)
    }

    fun modifyAt(position: Int, data: Intent): Unit? {
        val result: PhotoData = photoData.value?.getOrNull(position)?.run {
            if (video) {
                val modified: Boolean = data.getBooleanExtra(VideoEditActivity.MODIFIED, false)
                if(modified){
                    val videoEncodingArguments: VideoEditActivity.VideoEditArguments? = data.getSerializableExtra(VideoEditActivity.VIDEO_ARGUMENTS_TAG) as? VideoEditActivity.VideoEditArguments

                    sessionMap[imageUri]?.let { VideoEditActivity.cancelEncoding(it) }

                    videoEncodingArguments?.let {
                        videoEncodeStabilizationFirstPass = videoEncodingArguments.videoStabilize > 0.01f
                        videoEncodeProgress = 0

                        VideoEditActivity.startEncoding(imageUri, it,
                            context = getApplication<PixelDroidApplication>(),
                            registerNewFFmpegSession = ::registerNewFFmpegSession,
                            trackTempFile = ::trackTempFile,
                            videoEncodeProgress = ::videoEncodeProgress
                        )
                    }
                }
            } else {
                imageUri = data.getStringExtra(org.pixeldroid.media_editor.photoEdit.PhotoEditActivity.PICTURE_URI)!!.toUri()
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

    /**
     * @param originalUri the Uri of the file you sent to be edited
     * @param progress percentage of (this pass of) encoding that is done
     * @param firstPass Whether this is the first pass (currently for analysis of video stabilization) or the second (and last) pass.
     * @param outputVideoPath when not null, it means the encoding is done and the result is saved in this file
     * @param error is true when there has been an error during encoding.
     */
    private fun videoEncodeProgress(originalUri: Uri, progress: Int, firstPass: Boolean, outputVideoPath: Uri?, error: Boolean){
        photoData.value?.indexOfFirst { it.imageUri == originalUri }?.let { position ->

            if(outputVideoPath != null){
                // If outputVideoPath is not null, it means the video is done and we can change Uris
                val (size, _) = getSizeAndVideoValidate(outputVideoPath, position)

                photoData.value?.set(position,
                    photoData.value!![position].copy(
                        imageUri = outputVideoPath,
                        videoEncodeProgress = progress,
                        videoEncodeStabilizationFirstPass = firstPass,
                        videoEncodeComplete = true,
                        videoEncodeError = error,
                        size = size,
                    )
                )
            } else {
                photoData.value?.set(position,
                    photoData.value!![position].copy(
                        videoEncodeProgress = progress,
                        videoEncodeStabilizationFirstPass = firstPass,
                        videoEncodeComplete = false,
                        videoEncodeError = error,
                    )
                )
            }

            // Run assignment in main thread
            viewModelScope.launch {
                photoData.value = photoData.value
            }
        }
    }

    fun trackTempFile(file: File) {
        tempFiles.add(file)
    }

    fun cancelEncode(currentPosition: Int) {
        sessionMap[photoData.value?.getOrNull(currentPosition)?.imageUri]?.let { VideoEditActivity.cancelEncoding(it) }
    }

    override fun onCleared() {
        super.onCleared()
        VideoEditActivity.cancelEncoding()
        tempFiles.forEach {
            it.delete()
        }
    }

    fun registerNewFFmpegSession(position: Uri, sessionId: Long) {
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
