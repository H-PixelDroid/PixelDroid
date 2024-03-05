package org.pixeldroid.app.posts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.pixeldroid.app.utils.api.objects.Attachment
import javax.inject.Inject

data class AlbumUiState(
    val mediaAttachments: ArrayList<Attachment> = arrayListOf(),
    val index: Int = 0,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(state: SavedStateHandle) : ViewModel() {
    fun barHide() {
        _isActionBarHidden.update { !it }
    }

    companion object {
        const val ALBUM_IMAGES = "AlbumViewImages"
        const val ALBUM_INDEX = "AlbumViewIndex"
    }

    private val _uiState: MutableStateFlow<AlbumUiState>
    private val _isActionBarHidden: MutableStateFlow<Boolean>

    init {
        _uiState = MutableStateFlow(AlbumUiState(
            mediaAttachments = state[ALBUM_IMAGES] ?: ArrayList(),
            index = state[ALBUM_INDEX] ?: 0
        ))
        _isActionBarHidden = MutableStateFlow(false)
    }

    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()
    val isActionBarHidden: StateFlow<Boolean> = _isActionBarHidden
}