package org.pixeldroid.app.posts

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityReportBinding
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.objects.Status
import retrofit2.HttpException
import java.io.IOException

class ReportActivity : BaseActivity() {

    private lateinit var binding: ActivityReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.report)

        val status = intent.getSerializableExtra(Status.POST_TAG) as Status?

        binding.reportTargetTextview.text = getString(R.string.report_target).format(status?.account?.acct)


        binding.reportButton.setOnClickListener{
            binding.reportButton.visibility = View.INVISIBLE
            binding.reportProgressBar.visibility = View.VISIBLE

            binding.textInputLayout.editText?.isEnabled = false

            val api = apiHolder.api ?: apiHolder.setToCurrentUser()

            lifecycleScope.launchWhenCreated {
                try {
                    api.report(
                        status?.account?.id!!,
                        listOf(status),
                        binding.textInputLayout.editText?.text.toString()
                    )

                    reportStatus(true)
                } catch (exception: IOException) {
                    reportStatus(false)
                } catch (exception: HttpException) {
                    reportStatus(false)
                }
            }
        }
    }

    private fun reportStatus(success: Boolean){
        if(success){
            binding.reportProgressBar.visibility = View.GONE
            binding.reportButton.isEnabled = false
            binding.reportButton.text = getString(R.string.reported)
            binding.reportButton.visibility = View.VISIBLE
        } else {
            binding.textInputLayout.error = getString(R.string.report_error)
            binding.reportButton.visibility = View.VISIBLE
            binding.textInputLayout.editText?.isEnabled = true
            binding.reportProgressBar.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}