package com.h.pixeldroid.posts

import android.os.Bundle
import android.util.Log
import android.view.View
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.api.objects.Report
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.BaseActivity
import kotlinx.android.synthetic.main.activity_report.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.report)

        val status = intent.getSerializableExtra(Status.POST_TAG) as Status?

        //get the currently active user
        val user = db.userDao().getActiveUser()


        report_target_textview.text = getString(R.string.report_target).format(status?.account?.acct)


        reportButton.setOnClickListener{
            reportButton.visibility = View.INVISIBLE
            reportProgressBar.visibility = View.VISIBLE

            textInputLayout.editText?.isEnabled = false

            val accessToken = user?.accessToken.orEmpty()
            val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
            api.report("Bearer $accessToken", status?.account?.id!!, listOf(status), textInputLayout.editText?.text.toString())
                .enqueue(object : Callback<Report> {
                    override fun onResponse(
                        call: Call<Report>,
                        response: Response<Report>
                    ) {
                        if (response.body() == null || !response.isSuccessful) {
                            textInputLayout.error = getString(R.string.report_error)
                            reportButton.visibility = View.VISIBLE
                            textInputLayout.editText?.isEnabled = true
                            reportProgressBar.visibility = View.GONE
                        } else {
                            reportProgressBar.visibility = View.GONE
                            reportButton.isEnabled = false
                            reportButton.text = getString(R.string.reported)
                            reportButton.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<Report>, t: Throwable) {
                        Log.e("REPORT:", t.toString())
                    }
                })

        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}