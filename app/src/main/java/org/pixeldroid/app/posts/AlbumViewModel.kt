package org.pixeldroid.app.posts

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.pixeldroid.app.utils.api.objects.Attachment

data class AlbumUiState(
    val mediaAttachments: ArrayList<Attachment> = arrayListOf(),
    val index: Int = 0
)

class AlbumViewModel(application: Application, intent: Intent) : AndroidViewModel(application) {

    private val _uiState: MutableStateFlow<AlbumUiState>

    init {
        _uiState = MutableStateFlow(AlbumUiState(
            mediaAttachments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("images", arrayListOf<Attachment>()::class.java)!!
            } else {
                intent.getSerializableExtra("images") as ArrayList<Attachment>
            },
            index = intent.getIntExtra("index", 0)
        ))
    }

    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

}

class AlbumViewModelFactory(val application: Application, val intent: Intent) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, Intent::class.java).newInstance(application, intent)
    }
}