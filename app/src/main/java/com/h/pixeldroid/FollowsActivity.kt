package com.h.pixeldroid

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.AccountListFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWING_TAG
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FollowsActivity : AppCompatActivity() {
    var followsFragment = AccountListFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers)

        // Get account id
        val id = intent.getSerializableExtra(ACCOUNT_ID_TAG) as String?
        val following = intent.getSerializableExtra(FOLLOWING_TAG) as Boolean

        if(id == null) {
            val preferences = this.getSharedPreferences(
                "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
            )
            val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
            val accessToken = preferences.getString("accessToken", "")

            pixelfedAPI.verifyCredentials("Bearer $accessToken").enqueue(object :
                Callback<Account> {
                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("Cannot get account id", t.toString())
                }

                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if(response.code() == 200) {
                        val id = response.body()!!.id
                        launchActivity(id, following)
                    }
                }
            })
        } else {
            launchActivity(id, following)
        }
    }

    private fun launchActivity(id : String, following : Boolean) {val arguments = Bundle()
        arguments.putSerializable(ACCOUNT_ID_TAG, id)
        arguments.putSerializable(FOLLOWING_TAG, following)
        followsFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.followsFragment, followsFragment).commit()

    }
}
