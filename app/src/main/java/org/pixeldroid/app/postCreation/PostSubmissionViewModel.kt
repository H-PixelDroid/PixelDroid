package org.pixeldroid.app.postCreation

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.jarsilio.android.scrambler.exceptions.UnsupportedFileFormatException
import com.jarsilio.android.scrambler.stripMetadata
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
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import org.pixeldroid.app.utils.fileExtension
import org.pixeldroid.app.utils.getMimeType
import retrofit2.HttpException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import javax.inject.Inject


// Models the UI state for the PostCreationActivity
data class PostSubmissionActivityUiState(
    val userMessage: String? = null,

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
)

class PostSubmissionViewModel(application: Application, photodata: ArrayList<PhotoData>? = null, val existingDescription: String? = null) : AndroidViewModel(application) {
    private val photoData: MutableLiveData<MutableList<PhotoData>> by lazy {
        MutableLiveData<MutableList<PhotoData>>().also {
            if (photodata != null) {
                it.value =  photodata.toMutableList()
            }
        }
    }

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private val _uiState: MutableStateFlow<PostSubmissionActivityUiState>

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application)
        val initialDescription = sharedPreferences.getString("prefill_description", "") ?: ""

        _uiState = MutableStateFlow(PostSubmissionActivityUiState(newPostDescriptionText = existingDescription ?: initialDescription))
    }

    val uiState: StateFlow<PostSubmissionActivityUiState> = _uiState

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
                val orientation = ExifInterface(getApplication<PixelDroidApplication>().contentResolver.openInputStream(imageUri)!!).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

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

            //Ugly temporary account switching, but it works well enough for now
            val api = uiState.value.chosenAccount?.let {
                apiHolder.setToCurrentUser(it)
            } ?:  apiHolder.api ?: apiHolder.setToCurrentUser()

            val inter = api.mediaUpload(description, requestBody.parts[0])

            apiHolder.api = null
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

        //TODO investigate why this works but booleans don't
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

                api.postStatus(
                    statusText = description,
                    media_ids = getPhotoData().value!!.mapNotNull { it.uploadId }.toList(),
                    sensitive = nsfw
                )
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_success),
                    Toast.LENGTH_SHORT).show()
                val intent = Intent(getApplication(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                //TODO make the activity launch this instead (and surrounding toasts too)
                getApplication<PixelDroidApplication>().startActivity(intent)
            } catch (exception: HttpException) {
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_failed),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.response().toString() + exception.message().toString())
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        postCreationSendButtonEnabled = true
                    )
                }
            } catch (exception: Exception) {
                Toast.makeText(getApplication(), getApplication<PixelDroidApplication>().getString(R.string.upload_post_error),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, exception.toString())
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        postCreationSendButtonEnabled = true
                    )
                }
            }finally {
                apiHolder.api = null
            }
        }
    }

    fun newPostDescriptionChanged(text: Editable?) {
        _uiState.update { it.copy(newPostDescriptionText = text.toString()) }
    }

    fun trackTempFile(file: File) {
        tempFiles.add(file)
    }

    override fun onCleared() {
        super.onCleared()
        tempFiles.forEach {
            it.delete()
        }
    }

    fun updateNSFW(checked: Boolean) { _uiState.update { it.copy(nsfw = checked) } }

    fun chooseAccount(which: UserDatabaseEntity) {
        _uiState.update { it.copy(chosenAccount = which) }
    }
}


class PostSubmissionViewModelFactory(val application: Application, val photoData: ArrayList<PhotoData>, val existingDescription: String?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, ArrayList::class.java, String::class.java).newInstance(application, photoData, existingDescription)
    }
}