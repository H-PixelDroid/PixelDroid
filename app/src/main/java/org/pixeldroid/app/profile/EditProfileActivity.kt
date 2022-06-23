package org.pixeldroid.app.profile

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityEditProfileBinding
import org.pixeldroid.app.utils.BaseActivity

class EditProfileActivity : BaseActivity() {

    private lateinit var model: EditProfileViewModel
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.edit_profile)

        val _model: EditProfileViewModel by viewModels { EditProfileViewModelFactory(application) }
        model = _model

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    binding.savingProgressBar.visibility = if(uiState.loadingProfile) View.VISIBLE else View.INVISIBLE
                    binding.bioEditText.setText(uiState.bio)
                    binding.nameEditText.setText(uiState.name)
                    Glide.with(binding.profilePic).load(uiState.profilePictureUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.profilePic)
                    if(uiState.error){
                        Snackbar.make(binding.root, "Something went wrong",
                            Snackbar.LENGTH_LONG).show()
                        model.errorShown()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.action_apply -> {
                model.apply(
                    binding.nameEditText.text.toString(),
                    binding.bioEditText.text.toString(),
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
