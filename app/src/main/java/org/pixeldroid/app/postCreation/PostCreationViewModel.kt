package org.pixeldroid.app.postCreation

import android.content.ClipData
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PostCreationViewModel : ViewModel() {
    private val photoData: MutableLiveData<List<PhotoData>> by lazy {
        MutableLiveData<List<PhotoData>>().also {
            loadUsers()
        }
    }

    fun getUsers(): LiveData<List<PhotoData>> {
        return photoData
    }

    private fun loadUsers() {
        // Do an asynchronous operation to fetch users.
    }
}
class PostCreationViewModelFactory(val bundle: ClipData? = null) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(ClipData::class.java).newInstance(bundle)
    }

}