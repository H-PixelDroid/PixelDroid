package org.pixeldroid.app.posts

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityReportBinding
import org.pixeldroid.app.posts.ReportActivityViewModel.UploadState
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.objects.Status

class ReportActivity : BaseActivity() {

    private lateinit var binding: ActivityReportBinding
    private val model: ReportActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val status = intent.getSerializableExtra(Status.POST_TAG) as Status?

        binding.reportTargetTextview.text = getString(R.string.report_target).format(status?.account?.acct)

        binding.textInputLayout.editText?.text = model.editable

        binding.textInputLayout.editText?.doAfterTextChanged { model.textChanged(it) }

        binding.reportButton.setOnClickListener {
            model.sendReport(status, binding.textInputLayout.editText?.text.toString())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.reportSent.collect {
                    reportStatus(it)
                }
            }
        }
    }

    private fun reportStatus(success: UploadState){
        when (success) {
            UploadState.initial -> {
                binding.reportProgressBar.visibility = View.GONE
                binding.reportButton.visibility = View.VISIBLE
                binding.reportSuccess.visibility = View.INVISIBLE
            }
            UploadState.success -> {
                binding.reportProgressBar.visibility = View.GONE
                binding.reportButton.visibility = View.INVISIBLE
                binding.reportSuccess.visibility = View.VISIBLE
            }
            UploadState.failed -> {
                binding.textInputLayout.error = getString(R.string.report_error)
                binding.reportButton.visibility = View.VISIBLE
                binding.textInputLayout.editText?.isEnabled = true
                binding.reportProgressBar.visibility = View.GONE
                binding.reportSuccess.visibility = View.GONE
            }
            UploadState.inProgress -> {
                binding.reportButton.visibility = View.INVISIBLE
                binding.reportProgressBar.visibility = View.VISIBLE
                binding.textInputLayout.editText?.isEnabled = false
            }
        }
    }
}