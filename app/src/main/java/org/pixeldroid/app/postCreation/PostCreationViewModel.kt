package org.pixeldroid.app.postCreation

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.jarsilio.android.scrambler.exceptions.UnsupportedFileFormatException
import com.jarsilio.android.scrambler.stripMetadata
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.MultipartBody
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.fileExtension
import org.pixeldroid.app.utils.getMimeType
import org.pixeldroid.media_editor.photoEdit.VideoEditActivity
import retrofit2.HttpException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.arrayListOf
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.getOrNull
import kotlin.collections.indexOfFirst
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
    val maxEntries: Int?,

    val isCarousel: Boolean = true,

    val postCreationSendButtonEnabled: Boolean = true,

    val newPostDescriptionText: String = "",
    val nsfw: Boolean = false,

    val chosenAccount: UserDatabaseEntity? = null,

    val uploadProgressBarVisible: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadCompletedTextviewVisible: Boolean = false,
    val uploadErrorVisible: Boolean = false,
    val uploadErrorExplanationText: String = "",
    val uploadErrorExplanationVisible: Boolean = false,

    val storyCreation: Boolean,
    val storyDuration: Int = 10,
    val storyReplies: Boolean = true,
    val storyReactions: Boolean = true,
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
    var videoEncodeComplete: Boolean? = null,
    var videoEncodeError: Boolean = false,
) : Parcelable

class PostCreationViewModel(
    application: Application,
    clipdata: ClipData? = null,
    val instance: InstanceDatabaseEntity? = null,
    existingDescription: String? = null,
    existingNSFW: Boolean = false,
    storyCreation: Boolean = false,
) : AndroidViewModel(application) {
    private var storyPhotoDataBackup: MutableList<PhotoData>? = null
    private val photoData: MutableLiveData<MutableList<PhotoData>> by lazy {
        MutableLiveData<MutableList<PhotoData>>().also {
            it.value =  clipdata?.let { it1 -> addPossibleImages(it1, mutableListOf()) }
        }
    }

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private val _uiState: MutableStateFlow<PostCreationActivityUiState>

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application)
        val templateDescription = sharedPreferences.getString("prefill_description", "") ?: ""

        _uiState = MutableStateFlow(PostCreationActivityUiState(
            newPostDescriptionText = existingDescription ?: templateDescription,
            nsfw = existingNSFW,
            maxEntries = if(storyCreation) 1 else instance?.albumLimit,
            storyCreation = storyCreation
        ))
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

    /**
     * Read-only public view on [photoData]
     */
    fun getPhotoData(): LiveData<MutableList<PhotoData>> = photoData

    /**
     * Will add as many images as possible to [photoData], from the [clipData], and if
     * ([photoData].size + [clipData].itemCount) > uiState.value.maxEntries then it will only add as many images
     * as are legal (if any) and a dialog will be shown to the user alerting them of this fact.
     */
    fun addPossibleImages(clipData: ClipData, previousList: MutableList<PhotoData>? = photoData.value): MutableList<PhotoData> {
        val dataToAdd: ArrayList<PhotoData> = arrayListOf()
        var count = clipData.itemCount
        uiState.value.maxEntries?.let {
            if(count + (previousList?.size ?: 0) > it){
                _uiState.update { currentUiState ->
                    currentUiState.copy(userMessage = getApplication<PixelDroidApplication>().getString(R.string.total_exceeds_album_limit).format(it))
                }
                count = count.coerceAtMost(it - (previousList?.size ?: 0))
            }
            if (count + (previousList?.size ?: 0) >= it) {
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
    private fun getSizeAndVideoValidate(uri: Uri, editPosition: Int): Pair<Long, Boolean> {
        val size: Long =
            if (uri.scheme =="content") {
                getApplication<PixelDroidApplication>().contentResolver.query(uri, null, null, null, null)
                    ?.use { cursor ->
                        /* Get the column indexes of the data in the Cursor,
                         * move to the first row in the Cursor, get the data,
                         * and display it.
                         */
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if(sizeIndex >= 0) {
                            cursor.moveToFirst()
                            cursor.getLong(sizeIndex)
                        } else null
                    } ?: 0
            } else {
                uri.toFile().length()
            }

        val sizeInkBytes = ceil(size.toDouble() / 1000).toLong()
        val type = uri.getMimeType(getApplication<PixelDroidApplication>().contentResolver)
        val isVideo = type.startsWith("video/")

        if (isVideo && !instance!!.videoEnabled) {
            _uiState.update { currentUiState ->
                currentUiState.copy(userMessage = getApplication<PixelDroidApplication>().getString(R.string.video_not_supported))
            }
        }

        if ((!isVideo && sizeInkBytes > instance!!.maxPhotoSize) || (isVideo && sizeInkBytes > instance!!.maxVideoSize)) {
            //TODO Offer remedy for too big file: re-compress it
            val maxSize = if (isVideo) instance.maxVideoSize else instance.maxPhotoSize
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    userMessage = getApplication<PixelDroidApplication>().getString(R.string.size_exceeds_instance_limit, editPosition, sizeInkBytes, maxSize)
                )
            }
        }
        return Pair(size, isVideo)
    }

    fun updateDescription(position: Int, description: String) {
        photoData.value?.getOrNull(position)?.imageDescription = description
        photoData.value = photoData.value
    }

    fun removeAt(currentPosition: Int) {
        photoData.value?.removeAt(currentPosition)
        _uiState.update {
            it.copy(
                addPhotoButtonEnabled = (photoData.value?.size ?: 0) < (uiState.value.maxEntries ?: 0),
                )
        }
        photoData.value = photoData.value
    }

    fun modifyAt(position: Int, data: Intent): Unit? {
        val result: PhotoData = photoData.value?.getOrNull(position)?.run {
            if (video) {
                val modified: Boolean = data.getBooleanExtra(VideoEditActivity.MODIFIED, false)
                if(modified){
                    val videoEncodingArguments: VideoEditActivity.VideoEditArguments? = data.getSerializableExtra(VideoEditActivity.VIDEO_ARGUMENTS_TAG) as? VideoEditActivity.VideoEditArguments

                    sessionMap[imageUri]?.let { VideoEditActivity.cancelEncoding(it) }

                    videoEncodingArguments?.let {
                        videoEncodeStabilizationFirstPass = it.videoStabilize > 0.01f
                        videoEncodeProgress = 0
                        videoEncodeComplete = false

                        VideoEditActivity.startEncoding(imageUri, null, it,
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

            if (outputVideoPath != null) {
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

    private fun registerNewFFmpegSession(position: Uri, sessionId: Long) {
        sessionMap[position] = sessionId
    }

    fun becameCarousel(became: Boolean) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                isCarousel = became
            )
        }
    }

    fun resetUploadStatus() {
        photoData.value = photoData.value?.map { it.copy(uploadId = null, progress = null) }?.toMutableList()
    }

    /**
     * Uploads the images that are in the [photoData] array.
     * Keeps track of them in the [PhotoData.progress] (for the upload progress), and the
     * [PhotoData.uploadId] (for the list of ids of the uploads).
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun upload() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                postCreationSendButtonEnabled = false,
                uploadCompletedTextviewVisible = false,
                uploadErrorVisible = false,
                uploadProgressBarVisible = true
            )
        }

        for (data: PhotoData in getPhotoData().value ?: emptyList()) {
            val extension = data.imageUri.fileExtension(getApplication<PixelDroidApplication>().contentResolver)

            val strippedImage = File.createTempFile("temp_img", ".$extension", getApplication<PixelDroidApplication>().cacheDir)

            val imageUri = data.imageUri

            val (strippedOrNot, size) = try {
                val orientation = ExifInterface(getApplication<PixelDroidApplication>().contentResolver.openInputStream(imageUri)!!).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                stripMetadata(imageUri, strippedImage, getApplication<PixelDroidApplication>().contentResolver)

                // Restore EXIF orientation
                val exifInterface = ExifInterface(strippedImage)
                exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                exifInterface.saveAttributes()

                Pair(strippedImage.inputStream(), strippedImage.length())
            }  catch (e: UnsupportedFileFormatException){
                strippedImage.delete()
                if(imageUri != data.imageUri) File(URI(imageUri.toString())).delete()
                val imageInputStream = try {
                    getApplication<PixelDroidApplication>().contentResolver.openInputStream(imageUri)!!
                } catch (e: FileNotFoundException){
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            userMessage = getApplication<PixelDroidApplication>().getString(R.string.file_not_found,
                                data.imageUri)
                        )
                    }
                    return
                }
                Pair(imageInputStream, data.size)
            } catch (e: IOException){
                strippedImage.delete()
                if(imageUri != data.imageUri) File(URI(imageUri.toString())).delete()
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        userMessage = getApplication<PixelDroidApplication>().getString(R.string.file_not_found,
                            data.imageUri)
                    )
                }
                return
            }

            val type = data.imageUri.getMimeType(getApplication<PixelDroidApplication>().contentResolver)
            val imagePart = ProgressRequestBody(strippedOrNot, size, type)
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

            // Ugly temporary account switching, but it works well enough for now
            val api = uiState.value.chosenAccount?.let {
                apiHolder.setToCurrentUser(it)
            } ?:  apiHolder.api ?: apiHolder.setToCurrentUser()

            val inter: Observable<Attachment> =
                //TODO validate that image is correct (?) aspect ratio
                if (uiState.value.storyCreation) api.storyUpload(requestBody.parts[0])
                else api.mediaUpload(description, requestBody.parts[0])

            apiHolder.api = null
            postSub = inter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { attachment: Attachment ->
                        data.progress = 0
                        data.uploadId = if(uiState.value.storyCreation){
                            attachment.media_id!!
                        } else {
                             attachment.id!!
                        }
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
                        strippedImage.delete()
                        if(imageUri != data.imageUri) File(URI(imageUri.toString())).delete()
                        e.printStackTrace()
                        postSub?.dispose()
                        sub.dispose()
                    },
                    {
                        strippedImage.delete()
                        if(imageUri != data.imageUri) File(URI(imageUri.toString())).delete()
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

        // TODO: investigate why this works but booleans don't
        val nsfw = if (uiState.value.nsfw) 1 else 0

        _uiState.update { currentUiState ->
            currentUiState.copy(
                postCreationSendButtonEnabled = false
            )
        }
        viewModelScope.launch {
            try {
                //Ugly temporary account switching, but it works well enough for now
                val api = uiState.value.chosenAccount?.let {
                    apiHolder.setToCurrentUser(it)
                } ?:  apiHolder.api ?: apiHolder.setToCurrentUser()

                if(uiState.value.storyCreation){
                    val canReact = if (uiState.value.storyReactions) "1" else "0"
                    val canReply = if (uiState.value.storyReplies) "1" else "0"

                    api.storyPublish(
                        media_id = getPhotoData().value!!.firstNotNullOf { it.uploadId },
                        can_react = canReact,
                        can_reply = canReply,
                        duration = uiState.value.storyDuration
                    )
                } else {
                    api.postStatus(
                        statusText = description,
                        media_ids = getPhotoData().value!!.mapNotNull { it.uploadId }.toList(),
                        sensitive = nsfw
                    )
                }
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_success),
                    Toast.LENGTH_SHORT).show()
                val intent = Intent(getApplication(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                //TODO make the activity launch this instead (and surrounding toasts too)
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
            } finally {
                apiHolder.api = null
            }
        }
    }

    fun newPostDescriptionChanged(text: Editable?) {
        _uiState.update { it.copy(newPostDescriptionText = text.toString()) }
    }

    fun updateNSFW(checked: Boolean) { _uiState.update { it.copy(nsfw = checked) } }

    fun chooseAccount(which: UserDatabaseEntity) {
        _uiState.update { it.copy(chosenAccount = which) }
    }

    fun storyMode(storyMode: Boolean) {
        //TODO check ratio of files in story mode? What is acceptable?

        val newMaxEntries = if (storyMode) 1 else instance?.albumLimit
        var newUiState = _uiState.value.copy(
                storyCreation = storyMode,
                maxEntries = newMaxEntries,
                addPhotoButtonEnabled = (photoData.value?.size ?: 0) < (newMaxEntries ?: 0),
                )

        // Carousel on if in story mode
        if (storyMode) newUiState = newUiState.copy(isCarousel = true)

        // If switching to story, and there are too many pictures, keep the first and backup the rest
        if (storyMode && (photoData.value?.size ?: 0) > 1){
            storyPhotoDataBackup = photoData.value

            photoData.value = photoData.value?.let { mutableListOf(it.firstOrNull()).filterNotNull().toMutableList() }

            //Show message saying extraneous pictures were removed but can be restored
            newUiState = newUiState.copy(
                userMessage = getApplication<PixelDroidApplication>().getString(R.string.extraneous_pictures_stories)
            )
        }
        // Restore if backup not null and first value is unchanged
        else if (storyPhotoDataBackup != null && storyPhotoDataBackup?.firstOrNull() == photoData.value?.firstOrNull()){
            photoData.value = storyPhotoDataBackup
            storyPhotoDataBackup = null
        }
        _uiState.update { newUiState }
    }

    fun storyDuration(value: Int) {
        _uiState.update {
            it.copy(storyDuration = value)
        }
    }

    fun updateStoryReactions(checked: Boolean) { _uiState.update { it.copy(storyReactions = checked) }    }

    fun updateStoryReplies(checked: Boolean) { _uiState.update { it.copy(storyReplies = checked) }    }
}

class PostCreationViewModelFactory(val application: Application, val clipdata: ClipData, val instance: InstanceDatabaseEntity, val existingDescription: String?, val existingNSFW: Boolean, val storyCreation: Boolean) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, ClipData::class.java, InstanceDatabaseEntity::class.java, String::class.java, Boolean::class.java, Boolean::class.java).newInstance(application, clipdata, instance, existingDescription, existingNSFW, storyCreation)
    }
}
