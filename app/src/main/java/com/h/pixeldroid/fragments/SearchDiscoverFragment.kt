package com.h.pixeldroid.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.SearchActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.DiscoverPost
import com.h.pixeldroid.objects.DiscoverPosts
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : Fragment() {
    private lateinit var api: PixelfedAPI
    private lateinit var preferences: SharedPreferences
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : DiscoverRecyclerViewAdapter
    private lateinit var accessToken: String
    private lateinit var discoverProgressBar: ProgressBar
    private lateinit var discoverRefreshLayout: SwipeRefreshLayout



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
        preferences = requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        api = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "") ?: ""

        discoverProgressBar = view.findViewById(R.id.discoverProgressBar)
        discoverRefreshLayout = view.findViewById(R.id.discoverRefreshLayout)

        getDiscover()

        discoverRefreshLayout.setOnRefreshListener {
            getDiscover()
        }
    }

    private fun getDiscover() {

        api.discover("Bearer $accessToken")
            .enqueue(object : Callback<DiscoverPosts> {

                override fun onFailure(call: Call<DiscoverPosts>, t: Throwable) {
                    Log.e("SearchDiscoverFragment:", t.toString())
                }

                override fun onResponse(call: Call<DiscoverPosts>, response: Response<DiscoverPosts>) {
                    if(response.code() == 200) {
                        val discoverPosts = response.body()!!
                        adapter.addPosts(discoverPosts.posts)
                        discoverProgressBar.visibility = View.GONE
                        discoverRefreshLayout.isRefreshing = false
                    }
                }
            })
    }
    /**
     * [RecyclerView.Adapter] that can display a list of [DiscoverPost]s
     */
    class DiscoverRecyclerViewAdapter: RecyclerView.Adapter<DiscoverRecyclerViewAdapter.ViewHolder>() {
        private val posts: ArrayList<DiscoverPost> = ArrayList()

        fun addPosts(newPosts : List<DiscoverPost>) {
            posts.clear()
            posts.addAll(newPosts)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_profile_posts, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = posts[position]
            ImageConverter.setSquareImageFromURL(holder.postView, post.thumb, holder.postPreview)
            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postView.context, PostActivity::class.java)
                intent.putExtra(Status.DISCOVER_TAG, post)
                holder.postView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = posts.size

        inner class ViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
            val postPreview: ImageView = postView.findViewById(R.id.postPreview)
        }
    }
}
