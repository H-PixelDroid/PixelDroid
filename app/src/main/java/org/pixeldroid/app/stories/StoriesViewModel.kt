package org.pixeldroid.app.stories

import android.os.CountDownTimer
import android.text.Editable
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.utils.api.objects.CarouselUserContainer
import org.pixeldroid.app.utils.api.objects.Story
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.db.AppDatabase
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
@HiltViewModel
class StoriesViewModel @Inject constructor(state: SavedStateHandle,
                                           db: AppDatabase,
                                           private val apiHolder: PixelfedAPIHolder) : ViewModel() {
    private val carousel: StoryCarousel? = state[StoriesActivity.STORY_CAROUSEL]
    private val userId: String? = state[StoriesActivity.STORY_CAROUSEL_USER_ID]
    private val selfCarousel: Array<Story>? = state[StoriesActivity.STORY_CAROUSEL_SELF]

    private var currentAccount: CarouselUserContainer?

    private val _uiState: MutableStateFlow<StoriesUiState>

    val uiState: StateFlow<StoriesUiState>

    val count = MutableLiveData<Float>()

    private var timer: CountDownTimer? = null

    init {
        currentAccount =
        if (selfCarousel != null) {
            db.userDao().getActiveUser()?.let { CarouselUserContainer(it, selfCarousel.toList()) }
        } else carousel?.nodes?.firstOrNull { it?.user?.id == userId }

        _uiState = MutableStateFlow(newUiStateFromCurrentAccount())
        uiState = _uiState

        startTimerForCurrent()
    }

    private fun setTimer(timerLength: Float) {
        count.value = timerLength
        timer = object: CountDownTimer((timerLength * 1000).toLong(), 50){

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
            if(selfCarousel != null) return
            val currentUserId = currentAccount?.user?.id
            val currentAccountIndex = carousel?.nodes?.indexOfFirst { it?.user?.id == currentUserId } ?: return
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
