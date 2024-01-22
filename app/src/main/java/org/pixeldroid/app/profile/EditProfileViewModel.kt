package org.pixeldroid.app.profile

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.pixeldroid.app.postCreation.ProgressRequestBody
import org.pixeldroid.app.posts.fromHtml
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private val _uiState = MutableStateFlow(EditProfileActivityUiState())
    val uiState: StateFlow<EditProfileActivityUiState> = _uiState

    var oldProfile: Account? = null

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val api = apiHolder.api ?: apiHolder.setToCurrentUser()
            try {
                val profile = api.verifyCredentials()
                if (oldProfile == null) oldProfile = profile
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        name = oldProfile?.display_name,
                        bio = oldProfile?.source?.note,
                        profilePictureUri = oldProfile?.anyAvatar()?.toUri(),
                        privateAccount = oldProfile?.locked,
                        loadingProfile = false,
                        sendingProfile = false,
                        profileLoaded = true,
                        error = false
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        sendingProfile = false,
                        profileSent = false,
                        loadingProfile = false,
                        profileLoaded = false,
                        error = true
                    )
                }
            }
        }
    }

    fun sendProfile() {
        val api = apiHolder.api ?: apiHolder.setToCurrentUser()

        val requestBody =
            null //MultipartBody.Part.createFormData("avatar", System.currentTimeMillis().toString(), avatarBody)

        _uiState.update { currentUiState ->
            currentUiState.copy(
                sendingProfile = true,
                profileSent = false,
                loadingProfile = false,
                profileLoaded = false,
                error = false
            )
        }

        viewModelScope.launch {
            with(uiState.value) {
                try {
                    val account = api.updateCredentials(
                        displayName = name,
                        note = bio,
                        locked = privateAccount,
                    )
                    val newAvatarUri = if (profilePictureChanged) {
                        uploadImage()
                        profilePictureUri
                    } else {
                        account.anyAvatar()?.toUri()
                    }
                    oldProfile = account
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            bio = account.source?.note
                                ?: account.note?.let { fromHtml(it).toString() },
                            name = account.display_name,
                            profilePictureUri = newAvatarUri,
                            privateAccount = account.locked,
                            sendingProfile = false,
                            profileSent = true,
                            loadingProfile = false,
                            profileLoaded = true,
                            error = false
                        )
                    }
                } catch (exception: Exception) {
                    Log.e("TAG", exception.toString())
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            sendingProfile = false,
                            profileSent = false,
                            loadingProfile = false,
                            profileLoaded = false,
                            error = true
                        )
                    }
                }
            }
        }
    }

    fun updateBio(bio: Editable?) {
        _uiState.update { currentUiState ->
            currentUiState.copy(bio = bio.toString())
        }
    }

    fun updateName(name: Editable?) {
        _uiState.update { currentUiState ->
            currentUiState.copy(name = name.toString())
        }
    }

    fun updatePrivate(isChecked: Boolean) {
        _uiState.update { currentUiState ->
            currentUiState.copy(privateAccount = isChecked)
        }
    }

    fun changesApplied() {
        _uiState.update { currentUiState ->
            currentUiState.copy(profileLoaded = false)
        }
    }

    fun madeChanges(): Boolean =
        with(uiState.value) {
            val privateChanged = oldProfile?.locked != privateAccount
            val displayNameChanged = oldProfile?.display_name != name
            val bioChanged: Boolean = oldProfile?.source?.note?.let { it != bio }
            // If source note is null, check note
                ?: oldProfile?.note?.let { fromHtml(it).toString() != bio }
                ?: true
            profilePictureChanged || privateChanged || displayNameChanged || bioChanged
        }

    fun clickedCard() {
        if (uiState.value.error) {
            if (!uiState.value.profileLoaded) {
                // Load failed
                loadProfile()
            } else if (uiState.value.profileLoaded) {
                // Send failed
                sendProfile()
            }
        } else {
            // Dismiss success card
            _uiState.update { currentUiState ->
                currentUiState.copy(profileSent = false)
            }
        }
    }

    fun updateImage(image: String) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                profilePictureUri = image.toUri(),
                profilePictureChanged = true
            )
        }
    }

    private fun uploadImage() {
        val image = uiState.value.profilePictureUri!!

        val inputStream =
            getApplication<PixelDroidApplication>().contentResolver.openInputStream(image)
                ?: return

        val size: Long =
            if (image.scheme == "content") {
                getApplication<PixelDroidApplication>().contentResolver.query(
                    image,
                    null,
                    null,
                    null,
                    null
                )
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
                image.toFile().length()
            }

        val imagePart = ProgressRequestBody(inputStream, size, "image/*")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", System.currentTimeMillis().toString(), imagePart)
            .build()
        val sub = imagePart.progressSubject
            .subscribeOn(Schedulers.io())
            .subscribe { percentage ->
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        uploadProgress = percentage.toInt()
                    )
                }
            }

        var postSub: Disposable? = null

        val api = apiHolder.api ?: apiHolder.setToCurrentUser()
        val inter = api.updateProfilePicture(requestBody.parts[0])

        postSub = inter
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { it: Account ->
                    Log.i("ACCOUNT", it.toString())
                },
                { e: Throwable ->
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            uploadProgress = 0,
                            uploadingPicture = true,
                            error = true
                        )
                    }
                    e.printStackTrace()
                    postSub?.dispose()
                    sub.dispose()
                },
                {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            profilePictureChanged = false,
                            uploadProgress = 100,
                            uploadingPicture = false
                        )
                    }
                    postSub?.dispose()
                    sub.dispose()
                }
            )
    }
}


data class EditProfileActivityUiState(
    val name: String? = null,
    val bio: String? = null,
    val profilePictureUri: Uri? = null,
    val profilePictureChanged: Boolean = false,
    val privateAccount: Boolean? = null,
    val loadingProfile: Boolean = true,
    val profileLoaded: Boolean = false,
    val sendingProfile: Boolean = false,
    val profileSent: Boolean = false,
    val error: Boolean = false,
    val uploadingPicture: Boolean = false,
    val uploadProgress: Int = 0,
)

class EditProfileViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(application)
    }
}