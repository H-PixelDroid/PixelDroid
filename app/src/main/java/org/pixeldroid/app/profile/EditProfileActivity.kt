package org.pixeldroid.app.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityEditProfileBinding
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.openUrl

class EditProfileActivity : BaseActivity() {

    private lateinit var model: EditProfileViewModel
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val _model: EditProfileViewModel by viewModels { EditProfileViewModelFactory(application) }
        model = _model

        onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event
            if(model.madeChanges()){
                MaterialAlertDialogBuilder(binding.root.context).apply {
                    setMessage(getString(R.string.profile_save_changes))
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        this@addCallback.isEnabled = false
                        super.onBackPressedDispatcher.onBackPressed()
                    }
                }.show()
            } else {
                this.isEnabled = false
                super.onBackPressedDispatcher.onBackPressed()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    if(uiState.profileLoaded){
                        binding.bioEditText.setText(uiState.bio)
                        binding.nameEditText.setText(uiState.name)
                        model.changesApplied()
                    }
                    binding.progressCard.visibility = if(uiState.loadingProfile || uiState.sendingProfile || uiState.profileSent || uiState.error) View.VISIBLE else View.INVISIBLE
                    if(uiState.loadingProfile) binding.progressText.setText(R.string.fetching_profile)
                    else if(uiState.sendingProfile) binding.progressText.setText(R.string.saving_profile)
                    binding.privateSwitch.isChecked = uiState.privateAccount == true
                    Glide.with(binding.profilePic).load(uiState.profilePictureUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.profilePic)

                    binding.savingProgressBar.visibility = if(uiState.error || uiState.profileSent) View.GONE
                    else  View.VISIBLE

                    if(uiState.profileSent){
                        binding.progressText.setText(R.string.profile_saved)
                        binding.done.visibility = View.VISIBLE
                    } else {
                        binding.done.visibility = View.GONE
                    }
                    if(uiState.error){
                        binding.progressText.setText(R.string.error_profile)
                        binding.error.visibility = View.VISIBLE
                    } else binding.error.visibility = View.GONE

                }
            }
        }
        binding.bioEditText.doAfterTextChanged {
            model.updateBio(binding.bioEditText.text)
        }
        binding.nameEditText.doAfterTextChanged {
            model.updateName(binding.nameEditText.text)
        }
        binding.privateSwitch.setOnCheckedChangeListener { _, isChecked ->
            model.updatePrivate(isChecked)
        }

        binding.progressCard.setOnClickListener {
            model.clickedCard()
        }

        binding.editButton.setOnClickListener {
            val domain = db.userDao().getActiveUser()!!.instance_uri
            val url = "$domain/settings/home"

            if(!openUrl(url)) {
                Snackbar.make(binding.root, getString(R.string.edit_link_failed),
                    Snackbar.LENGTH_LONG).show()
            }
        }

        binding.profilePic.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
                action = Intent.ACTION_GET_CONTENT
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                uploadImageResultContract.launch(
                    Intent.createChooser(this, null)
                )
            }
        }
    }

    private val uploadImageResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val images: ArrayList<String> = ArrayList()
            val clipData = data.clipData
            if (clipData != null) {
                val count = clipData.itemCount
                for (i in 0 until count) {
                    val imageUri: String = clipData.getItemAt(i).uri.toString()
                    images.add(imageUri)
                }
                model.updateImage(images.first())
            } else if (data.data != null) {
                images.add(data.data!!.toString())
                model.updateImage(images.first())
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
                model.sendProfile()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
