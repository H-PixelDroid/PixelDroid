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
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ProfilePostFragment :  Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var pixelfedAPI: PixelfedAPI
    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    private var accessToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_post, container, false)
        preferences = requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")

        // Set posts RecyclerView as a grid with 3 columns
        recycler = view.findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfilePostsRecyclerViewAdapter(requireContext())
        recycler.adapter = adapter

        setPosts()

        return view
    }

    private fun setPosts() {
        val account = arguments?.getSerializable(Account.ACCOUNT_TAG) as Account?

        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account!!.id)
            .enqueue(object : Callback<List<Status>> {

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("PROFILE POSTS", t.toString())
                }

                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if(response.code() == 200) {
                        val posts = ArrayList<Status>()
                        val statuses = response.body()!!
                        for(status in statuses) {
                            posts.add(status)
                        }
                        adapter.addPosts(posts)
                    } else {
                        Log.e("POSTS:", response.code().toString())
                    }
                }
            })
    }
}