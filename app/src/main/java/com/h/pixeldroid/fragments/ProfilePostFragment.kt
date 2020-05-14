package com.h.pixeldroid.fragments

import android.util.Log
import com.h.pixeldroid.adapters.ProfilePostsRecyclerViewAdapter
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ProfilePostFragment : ProfileTabsFragment() {

    override fun setPosts(
        adapter: ProfilePostsRecyclerViewAdapter, pixelfedAPI: PixelfedAPI,
        accessToken: String?) {

        val account = arguments?.getSerializable(Account.ACCOUNT_TAG) as Account?

        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account!!.id)
            .enqueue(object : Callback<List<Status>> {

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("PROFILE POSTS", t.toString())
                }

                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    handleAPIResponse(response)
                }
            })
    }
}