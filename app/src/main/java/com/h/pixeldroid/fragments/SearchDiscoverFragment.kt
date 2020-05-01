package com.h.pixeldroid.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.SearchActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.DiscoverPosts
import kotlinx.android.synthetic.main.fragment_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : Fragment() {
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : DiscoverRecyclerViewAdapter



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val button = view.findViewById<Button>(R.id.searchButton)
        val search = view.findViewById<EditText>(R.id.searchEditText)
        button.setOnClickListener {
            val intent = Intent(context, SearchActivity::class.java)
            intent.putExtra("searchFeed", search.text.toString())
            startActivity(intent)
        }
        // Set posts RecyclerView as a grid with 3 columns
        recycler = view.findViewById(R.id.discoverList)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = DiscoverRecyclerViewAdapter()
        recycler.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getDiscover()
    }

    private fun getDiscover() {
        val preferences = requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        val api = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "") ?: ""

        api.discover("Bearer $accessToken")
            .enqueue(object : Callback<DiscoverPosts> {

                override fun onFailure(call: Call<DiscoverPosts>, t: Throwable) {
                    Log.e("SearchDiscoverFragment:", t.toString())
                }

                override fun onResponse(call: Call<DiscoverPosts>, response: Response<DiscoverPosts>) {
                    if(response.code() == 200) {
                        val discoverPosts = response.body()!!
                        adapter.addPosts(discoverPosts.posts)
                        searchProgressBar.visibility = View.GONE
                    }
                }
            })    }
}
