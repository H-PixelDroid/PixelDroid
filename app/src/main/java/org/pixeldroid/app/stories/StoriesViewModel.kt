package org.pixeldroid.app.stories

import android.app.Application
import android.os.CountDownTimer
import android.text.Editable
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import java.time.Instant
import javax.inject.Inject

data class StoriesUiState(
    val profilePicture: String? = null,
    val username: String? = null,
    val age: Instant? = null,
    val currentImage: Int = 0,
    val imageList: List<String> = emptyList(),
    val durationList: List<Int> = emptyList(),
    val paused: Boolean = false,
    @StringRes
    val errorMessage: Int? = null,
    @StringRes
    val snackBar: Int? = null,
    val reply: String = ""
)

class StoriesViewModel(
    application: Application,
    val carousel: StoryCarousel,
    userId: String?
) : AndroidViewModel(application) {

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private var currentAccount = carousel.nodes?.firstOrNull { it?.user?.id == userId }

    private val _uiState: MutableStateFlow<StoriesUiState> = MutableStateFlow(
        newUiStateFromCurrentAccount()
    )

    val uiState: StateFlow<StoriesUiState> = _uiState

    val count = MutableLiveData<Float>()

    private var timer: CountDownTimer? = null

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        startTimerForCurrent()
    }

    private fun setTimer(timerLength: Float) {
        count.value = timerLength
        timer = object: CountDownTimer((timerLength * 1000).toLong(), 100){

            override fun onTick(millisUntilFinished: Long) {
                count.value = millisUntilFinished.toFloat() / 1000
            }

            override fun onFinish() {
                goToNext()
            }
        }
    }

    private fun newUiStateFromCurrentAccount(): StoriesUiState = StoriesUiState(
        profilePicture = currentAccount?.user?.avatar,
        age = currentAccount?.nodes?.getOrNull(0)?.created_at,
        username = currentAccount?.user?.username, //TODO check if not username_acct, think about falling back on other option?
        errorMessage = null,
        currentImage = 0,
        imageList = currentAccount?.nodes?.mapNotNull { it?.src } ?: emptyList(),
        durationList = currentAccount?.nodes?.mapNotNull { it?.duration } ?: emptyList()
    )

    private fun goTo(index: Int){
        if((0 until uiState.value.imageList.size).contains(index)) {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    currentImage = index,
                    age = currentAccount?.nodes?.getOrNull(index)?.created_at,
                    paused = false
                )
            }
        } else {
            val currentUserId = currentAccount?.user?.id
            val currentAccountIndex = carousel.nodes?.indexOfFirst { it?.user?.id == currentUserId } ?: return
            currentAccount = when (index) {
                uiState.value.imageList.size -> {
                    // Go to next user
                    if(currentAccountIndex + 1 >= carousel.nodes.size) return
                    carousel.nodes.getOrNull(currentAccountIndex + 1)

                }

                -1 -> {
                    // Go to previous user
                    if(currentAccountIndex <= 0) return
                    carousel.nodes.getOrNull(currentAccountIndex - 1)
                }
                else -> return // Do nothing, given index does not make sense
            }
            _uiState.update { newUiStateFromCurrentAccount() }
        }

        timer?.cancel()
        startTimerForCurrent()
    }

    fun goToNext() {
        viewModelScope.launch {
            try {
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                val story = currentAccount?.nodes?.getOrNull(uiState.value.currentImage)

                if (story?.seen == true){
                    //TODO update seen when marked successfully as seen?
                    story.id?.let { api.storySeen(it) }
                }
            } catch (exception: Exception){
                _uiState.update { currentUiState ->
                    currentUiState.copy(errorMessage = R.string.story_could_not_see)
                }
            }

        }
        goTo(uiState.value.currentImage + 1)
    }

    fun goToPrevious() = goTo(uiState.value.currentImage - 1)

    private fun startTimerForCurrent(){
        uiState.value.let {
            it.durationList.getOrNull(it.currentImage)?.toLong()?.let { time ->
                setTimer(time.toFloat())
                timer?.start()
            }
        }
    }

    fun pause() {
        if(_uiState.value.paused){
            timer?.start()
        } else {
            timer?.cancel()
            count.value?.let { setTimer(it) }
        }
        _uiState.update { currentUiState ->
            currentUiState.copy(paused = !currentUiState.paused)
        }
    }

    fun sendReply(text: Editable) {
        viewModelScope.launch {
            try {
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                currentStoryId()?.let { api.storyComment(it, text.toString()) }

                _uiState.update { currentUiState ->
                    currentUiState.copy(snackBar = R.string.sent_reply_story)
                }
            } catch (exception: Exception){
                _uiState.update { currentUiState ->
                    currentUiState.copy(errorMessage = R.string.story_reply_error)
                }
            }

        }
    }

    private fun currentStoryId(): String? = currentAccount?.nodes?.getOrNull(uiState.value.currentImage)?.id

    fun replyChanged(text: String) {
        _uiState.update { currentUiState ->
            currentUiState.copy(reply = text)
        }
    }

    fun dismissError() {
        _uiState.update { currentUiState ->
            currentUiState.copy(errorMessage = null)
        }
    }

    fun shownSnackbar() {
        _uiState.update { currentUiState ->
            currentUiState.copy(snackBar = null)
        }
    }

    fun currentProfileId(): String? = currentAccount?.user?.id

}

class StoriesViewModelFactory(
    val application: Application,
    val carousel: StoryCarousel,
    val userId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, StoryCarousel::class.java, String::class.java).newInstance(application, carousel, userId)
    }
}
