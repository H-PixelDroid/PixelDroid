package com.h.pixeldroid.fragments

import android.util.Log
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ProfileBookmarkFragment : ProfileTabsFragment() {

    override fun setPosts(
        adapter: ProfilePostsRecyclerViewAdapter, pixelfedAPI: PixelfedAPI,
        accessToken: String?, requestedLoadSize: Int): Call<List<Status>> {

        return pixelfedAPI.bookmarkedPosts("Bearer $accessToken", limit = "$requestedLoadSize")
    }
}