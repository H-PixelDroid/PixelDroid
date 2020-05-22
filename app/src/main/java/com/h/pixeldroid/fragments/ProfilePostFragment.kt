package com.h.pixeldroid.fragments

import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call

internal class ProfilePostFragment : ProfileTabsFragment() {

    override fun setPosts(
        adapter: ProfilePostsRecyclerViewAdapter, pixelfedAPI: PixelfedAPI,
        accessToken: String?, requestedLoadSize: Int): Call<List<Status>> {

        val account = arguments?.getSerializable(Account.ACCOUNT_TAG) as Account?

        return pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account!!.id)
    }
}