package com.h.pixeldroid

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.di.PixelfedAPIHolder
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.objects.DiscoverPost
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Status.Companion.DISCOVER_TAG
import com.h.pixeldroid.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.utils.DBUtils
import kotlinx.android.synthetic.main.activity_post.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class PostActivity : AppCompatActivity() {
    private lateinit var postFragment : PostFragment
    lateinit var domain : String
    private lateinit var accessToken : String
    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        (this.application as Pixeldroid).getAppComponent().inject(this)


        val status = intent.getSerializableExtra(POST_TAG) as Status?
        val discoverPost: DiscoverPost? = intent.getSerializableExtra(DISCOVER_TAG) as DiscoverPost?

        val user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        accessToken = user?.accessToken.orEmpty()

        postFragment = PostFragment()
        val arguments = Bundle()
        arguments.putString(DOMAIN_TAG, domain)

        if (discoverPost != null) {
            postProgressBar.visibility = View.VISIBLE
            getDiscoverPost(arguments, discoverPost)
        } else {
            initializeFragment(arguments, status)
        }
    }

    private fun getDiscoverPost(
        arguments: Bundle,
        discoverPost: DiscoverPost
    ) {
        val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        val id = discoverPost.url?.substringAfterLast('/') ?: ""
        api.getStatus("Bearer $accessToken", id).enqueue(object : Callback<Status> {

            override fun onFailure(call: Call<Status>, t: Throwable) {
                Log.e("PostActivity:", t.toString())
            }

            override fun onResponse(call: Call<Status>, response: Response<Status>) {
                if(response.code() == 200) {
                    val status = response.body()!!
                    postProgressBar.visibility = View.GONE
                    initializeFragment(arguments, status)
                }
            }
        })
    }

    private fun initializeFragment(arguments: Bundle, status: Status?){
        arguments.putSerializable(POST_TAG, status)
        postFragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .add(R.id.postFragmentSingle, postFragment).commit()
        postFragmentSingle.visibility = View.VISIBLE
    }
}
