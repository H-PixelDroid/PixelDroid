package org.pixeldroid.app.postCreation

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentPostSubmissionBinding
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.setSquareImageFromURL


class PostSubmissionFragment : BaseFragment() {

    private lateinit var accounts: List<UserDatabaseEntity>
    private var selectedAccount: Int = -1
//    private lateinit var menu: Menu

    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private lateinit var binding: FragmentPostSubmissionBinding
    private lateinit var model: PostCreationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        // Inflate the layout for this fragment
        binding = FragmentPostSubmissionBinding.inflate(layoutInflater)
//        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topBar.setupWithNavController(findNavController())

        user = db.userDao().getActiveUser()
        accounts = db.userDao().getAll()

        instance = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        val _model: PostCreationViewModel by activityViewModels {
            PostCreationViewModelFactory(
                requireActivity().application,
                requireActivity().intent.clipData!!,
                instance,
                requireActivity().intent.getStringExtra(PostCreationActivity.PICTURE_DESCRIPTION),
                requireActivity().intent.getBooleanExtra(PostCreationActivity.POST_NSFW, false)
            )
        }
        model = _model

        // Display the values from the view model
        binding.nsfwSwitch.isChecked = model.uiState.value.nsfw
        binding.newPostDescriptionInputField.setText(model.uiState.value.newPostDescriptionText)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    uiState.userMessage?.let {
                        AlertDialog.Builder(binding.root.context).apply {
                            setMessage(it)
                            setNegativeButton(android.R.string.ok) { _, _ -> }
                        }.show()

                        // Notify the ViewModel the message is displayed
                        model.userMessageShown()
                    }
                    enableButton(uiState.postCreationSendButtonEnabled)
                    binding.uploadProgressBar.visibility =
                        if (uiState.uploadProgressBarVisible) View.VISIBLE else View.INVISIBLE
                    binding.uploadProgressBar.progress = uiState.uploadProgress
                    binding.uploadCompletedTextview.visibility =
                        if (uiState.uploadCompletedTextviewVisible) View.VISIBLE else View.INVISIBLE
                    binding.uploadError.visibility =
                        if (uiState.uploadErrorVisible) View.VISIBLE else View.INVISIBLE
                    binding.uploadErrorTextExplanation.visibility =
                        if (uiState.uploadErrorExplanationVisible) View.VISIBLE else View.INVISIBLE

                    selectedAccount = accounts.indexOf(uiState.chosenAccount)

                    binding.uploadErrorTextExplanation.text = uiState.uploadErrorExplanationText
                }
            }
        }

        binding.newPostDescriptionInputField.doAfterTextChanged {
            model.newPostDescriptionChanged(binding.newPostDescriptionInputField.text)
        }

        binding.nsfwSwitch.setOnCheckedChangeListener { _, isChecked ->
            model.updateNSFW(isChecked)
        }

        binding.postTextInputLayout.counterMaxLength = instance.maxStatusChars

        setSquareImageFromURL(View(requireActivity()), model.getPhotoData()!!.value?.get(0)?.imageUri.toString(), binding.postPreview)

        // Get the description and send the post
        binding.postCreationSendButton.setOnClickListener {
            if (validatePost()) model.upload()
        }

        // Button to retry image upload when it fails
        binding.retryUploadButton.setOnClickListener {
            model.resetUploadStatus()
            model.upload()
        }

        // Handle back pressed button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_postSubmissionFragment_to_postCreationFragment)
            }
        })

        binding.topBar.addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.post_submission_account_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_switch_accounts -> {
                        AlertDialog.Builder(requireActivity()).apply {
                            setIcon(R.drawable.switch_account)
                            setTitle(R.string.switch_accounts)
                            setSingleChoiceItems(accounts.map { it.username + " (${it.fullHandle})" }.toTypedArray(), selectedAccount) { dialog, which ->
                                if (selectedAccount != which) {
                                    model.chooseAccount(accounts[which])
                                }
                                dialog.dismiss()
                            }
                            setNegativeButton(android.R.string.cancel) { _, _ -> }
                        }.show()
                        return true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun validatePost(): Boolean {
        binding.postTextInputLayout.run {
            val content = editText?.length() ?: 0
            if (content > counterMaxLength) {
                // error, too many characters
                error = resources.getQuantityString(R.plurals.description_max_characters, counterMaxLength, counterMaxLength)
                return false
            }
        }
        return true
    }

    private fun enableButton(enable: Boolean = true){
        binding.postCreationSendButton.isEnabled = enable
        if(enable){
            binding.postingProgressBar.visibility = View.GONE
            binding.postCreationSendButton.visibility = View.VISIBLE
        } else {
            binding.postingProgressBar.visibility = View.VISIBLE
            binding.postCreationSendButton.visibility = View.GONE
        }
    }

}