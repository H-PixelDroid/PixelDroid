package org.pixeldroid.app.stories

import android.app.Application
import android.os.CountDownTimer
import android.text.Editable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
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
    val errorMessage: String? = null,
    val snackBar: String? = null,
    val reply: String = ""
)

class StoriesViewModel(
    application: Application,
) : AndroidViewModel(application) {

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    private val _uiState: MutableStateFlow<StoriesUiState> = MutableStateFlow(StoriesUiState())

    val uiState: StateFlow<StoriesUiState> = _uiState

    var carousel: StoryCarousel? = null

    val count = MutableLiveData<Long>()

    private var timer: CountDownTimer? = null

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        loadStories()
    }

    private fun setTimer(timerLength: Long) {
        count.value = timerLength
        timer = object: CountDownTimer(timerLength * 1000, 500){

            override fun onTick(millisUntilFinished: Long) {
                count.value = millisUntilFinished / 1000
                Log.e("Timer second", "${count.value}")
            }

            override fun onFinish() {
                goToNext()
            }
        }
    }

    private fun goToNext(){
        _uiState.update { currentUiState ->
            currentUiState.copy(
                currentImage = currentUiState.currentImage + 1,
                //TODO don't just take the first here, choose from activity input somehow?
                age = carousel?.nodes?.firstOrNull()?.nodes?.getOrNull(currentUiState.currentImage + 1)?.created_at
            )
        }
        //TODO when done with viewing all stories, close activity and move to profile (?)
        timer?.cancel()
        startTimerForCurrent()
    }

    private fun loadStories() {
        viewModelScope.launch {
            try{
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                carousel = api.carousel()

                //TODO don't just take the first here, choose from activity input somehow?
                val chosenAccount = carousel?.nodes?.firstOrNull()

                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        profilePicture = chosenAccount?.user?.avatar,
                        age = chosenAccount?.nodes?.getOrNull(0)?.created_at,
                        username = chosenAccount?.user?.username, //TODO check if not username_acct, think about falling back on other option?
                        errorMessage = null,
                        currentImage = 0,
                        imageList = chosenAccount?.nodes?.mapNotNull { it?.src } ?: emptyList(),
                        durationList = chosenAccount?.nodes?.mapNotNull { it?.duration } ?: emptyList()
                    )
                }
                startTimerForCurrent()
            } catch (exception: Exception){
                _uiState.update { currentUiState ->
                    currentUiState.copy(errorMessage = "Something went wrong fetching the carousel")
                }
            }
        }
    }

    private fun startTimerForCurrent(){
        uiState.value.let {
            it.durationList.getOrNull(it.currentImage)?.toLong()?.let { time ->
                setTimer(time)
                timer?.start()
            }
        }
    }

    fun imageLoaded() {/*
        _uiState.update { currentUiState ->
            currentUiState.copy(currentImage = currentUiState.currentImage + 1)
        }*/
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
                //TODO don't just take the first here, choose from activity input somehow?
                val id = carousel?.nodes?.firstOrNull()?.nodes?.getOrNull(uiState.value.currentImage)?.id
                id?.let { api.storyComment(it, text.toString()) }

                _uiState.update { currentUiState ->
                    currentUiState.copy(snackBar = "Sent reply")
                }
            } catch (exception: Exception){
                _uiState.update { currentUiState ->
                    currentUiState.copy(errorMessage = "Something went wrong sending reply")
                }
            }

        }
    }

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
}

class StoriesViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(application)
    }
}
