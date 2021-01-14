package com.h.pixeldroid.posts

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityPostBinding
import com.h.pixeldroid.utils.api.objects.DiscoverPost
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.api.objects.Status.Companion.DISCOVER_TAG
import com.h.pixeldroid.utils.api.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.utils.api.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.utils.BaseActivity
import retrofit2.HttpException
import java.io.IOException

class PostActivity : BaseActivity() {
    private lateinit var postFragment : PostFragment
    lateinit var domain : String
    private lateinit var accessToken : String

    private lateinit var binding: ActivityPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val status = intent.getSerializableExtra(POST_TAG) as Status?
        val discoverPost: DiscoverPost? = intent.getSerializableExtra(DISCOVER_TAG) as DiscoverPost?

        val user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        accessToken = user?.accessToken.orEmpty()

        postFragment = PostFragment()
        val arguments = Bundle()
        arguments.putString(DOMAIN_TAG, domain)

        if (discoverPost != null) {
            binding.postProgressBar.visibility = View.VISIBLE
            getDiscoverPost(arguments, discoverPost)
        } else {
            initializeFragment(arguments, status)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getDiscoverPost(
        arguments: Bundle,
        discoverPost: DiscoverPost
    ) {
        val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        val id = discoverPost.url?.substringAfterLast('/') ?: ""
        lifecycleScope.launchWhenCreated {
            try {
                val status = api.getStatus("Bearer $accessToken", id)
                binding.postProgressBar.visibility = View.GONE
                initializeFragment(arguments, status)
            }  catch (exception: IOException) {
                //TODO show error message
                Log.e("PostActivity:", exception.toString())
            } catch (exception: HttpException) {
            }
        }
    }

    private fun initializeFragment(arguments: Bundle, status: Status?){
        supportActionBar?.title = getString(R.string.post_title).format(status!!.account?.getDisplayName())
        arguments.putSerializable(POST_TAG, status)
        postFragment.arguments = arguments
        supportFragmentManager.isStateSaved
        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
        binding.postFragmentSingle.visibility = View.VISIBLE
    }
}
