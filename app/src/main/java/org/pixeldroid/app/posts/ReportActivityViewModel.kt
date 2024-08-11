package org.pixeldroid.app.posts

import android.text.Editable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

@HiltViewModel
class ReportActivityViewModel @Inject constructor(val apiHolder: PixelfedAPIHolder): ViewModel() {
    var editable: Editable? = null
        private set

    private val _reportSent: MutableStateFlow<UploadState> = MutableStateFlow(UploadState.initial)
    val reportSent = _reportSent.asStateFlow()

    enum class UploadState {
        initial, success, failed, inProgress
    }
    fun textChanged(it: Editable?) {
        editable = it
    }

    fun sendReport(status: Status?, text: String) {
        _reportSent.value = UploadState.inProgress
        viewModelScope.launch {
            val api = apiHolder.api ?: apiHolder.setToCurrentUser()
            try {
                api.report(
                    status?.account?.id!!,
                    listOf(status),
                    text
                )

                _reportSent.value = UploadState.success
            } catch (exception: Exception) {
                _reportSent.value = UploadState.failed
            }
        }
    }
}