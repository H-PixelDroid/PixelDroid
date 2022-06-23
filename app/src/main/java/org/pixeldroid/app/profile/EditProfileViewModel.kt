package org.pixeldroid.app.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception
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
                oldProfile = api.verifyCredentials()
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        name = oldProfile?.display_name,
                        bio = oldProfile?.source?.note,
                        profilePictureUri = oldProfile?.anyAvatar()?.toUri(),
                        privateAccount = oldProfile?.locked,
                        loadingProfile = false,
                        sendingProfile = false
                    )
                }
            } catch (exception: IOException) {
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                    )
                }
            } catch (exception: HttpException) {
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                    )
                }
            }
        }
    }

    fun apply(name: String, bio: String) {
        //TODO check if name and bio have changed, else send null to updatecredentials or don't update at all
        _uiState.update { currentUiState ->
            if(oldProfile != null) currentUiState.copy(name = name, bio = bio, sendingProfile = true, loadingProfile = false)
            else currentUiState.copy(name = name, bio = bio, sendingProfile = false)
        }
        if(oldProfile == null) return

        val api = apiHolder.api ?: apiHolder.setToCurrentUser()

        val requestBody = null //MultipartBody.Part.createFormData("avatar", System.currentTimeMillis().toString(), avatarBody)

        viewModelScope.launch {
            with(uiState.value) {
                try {
                    api.updateCredentials(
                        displayName = name,
                        note = bio,
                        locked = privateAccount,
                      //  avatar = requestBody
                    )
                } catch (exception: IOException) {
                    Log.e("TAG", exception.toString())
                    _uiState.update { currentUiState ->
                        currentUiState.copy(error = true)
                    }
                } catch (exception: HttpException) {
                    Log.e("TAG", exception.toString())
                    _uiState.update { currentUiState ->
                        currentUiState.copy(error = true)
                    }
                } catch (exception: Exception) {
                    Log.e("TAG", exception.toString())

                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { currentUiState ->
            currentUiState.copy(error = false)
        }
    }
}

data class EditProfileActivityUiState(
    val name: String? = null,
    val bio: String? = null,
    val profilePictureUri: Uri?= null,
    val privateAccount: Boolean? = null,
    val loadingProfile: Boolean = true,
    val sendingProfile: Boolean = false,
    val error: Boolean = false
)

class EditProfileViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(application)
    }
}