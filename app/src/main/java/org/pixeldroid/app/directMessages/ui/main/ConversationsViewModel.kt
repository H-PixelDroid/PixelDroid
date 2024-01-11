package org.pixeldroid.app.directMessages.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import javax.inject.Inject

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {
    // TODO: Implement the ViewModel
    // API calls for DM, store results in some variable here
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    init {
        (application as PixelDroidApplication).getAppComponent().inject(this)
    }

    suspend fun loadConversations() {
        val api = apiHolder.api ?: apiHolder.setToCurrentUser()
        val conversations = api.viewAllConversations()
    }
    

}