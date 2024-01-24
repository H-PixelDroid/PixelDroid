package org.pixeldroid.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import javax.inject.Inject

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var db: AppDatabase

    // Mutable state flow that will be used internally in the ViewModel, empty list is given as initial value.
    private val _users = MutableStateFlow(emptyList<UserDatabaseEntity>())

    // Immutable state flow exposed to UI
    val users = _users.asStateFlow()


    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
        getUsers()
    }

    private fun getUsers() {
        viewModelScope.launch {
            db.userDao().getAllFlow().flowOn(Dispatchers.IO)
                .collect { users: List<UserDatabaseEntity> ->
                    _users.update { users }
                }
        }
    }
}

class MainActivityViewModelFactory(
    val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(application)
    }
}
