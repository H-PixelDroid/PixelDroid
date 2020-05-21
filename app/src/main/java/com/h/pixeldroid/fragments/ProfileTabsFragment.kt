package com.h.pixeldroid.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.adapters.ProfilePostsRecyclerViewAdapter
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import retrofit2.Response

abstract class ProfileTabsFragment : Fragment() {
    internal lateinit var pixelfedAPI: PixelfedAPI
    internal lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    internal var accessToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_post, container, false)

        val db = DBUtils.initDB(requireContext())

        val user = db.userDao().getActiveUser()

        val domain = user?.instance_uri.orEmpty()
        pixelfedAPI = PixelfedAPI.create(domain)
        accessToken = user?.accessToken.orEmpty()

        // Set posts RecyclerView as a grid with 3 columns
        recycler = view.findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfilePostsRecyclerViewAdapter(requireContext())
        recycler.adapter = adapter

        setPosts(adapter, pixelfedAPI, accessToken)

        return view
    }

    protected abstract fun setPosts(
        adapter: ProfilePostsRecyclerViewAdapter, pixelfedAPI: PixelfedAPI,
        accessToken: String?)

    protected fun handleAPIResponse(response: Response<List<Status>>) {
        if(response.code() == 200) {
            val posts = ArrayList<Status>()
            val statuses = response.body()!!
            for(status in statuses) {
                posts.add(status)
            }
            adapter.addPosts(posts)
        } else {
            Log.e("PROFILE POSTS: ", response.code().toString())
        }
    }
}
