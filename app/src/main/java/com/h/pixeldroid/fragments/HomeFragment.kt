package com.h.pixeldroid.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.FeedRecyclerViewAdapter
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    var feed : RecyclerView? = null
    var adapter : FeedRecyclerViewAdapter? = null
    var posts : List<Post> = ArrayList()

    fun setContent(newPosts : ArrayList<Post>, view : View) {
        feed = view.findViewById(R.id.feedList)
        feed?.setHasFixedSize(true)
        feed?.layoutManager = LinearLayoutManager(context)
        posts = newPosts
        adapter = FeedRecyclerViewAdapter(posts, context!!)
        feed?.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferences = this.activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pixelfedAPI = PixelfedAPI.create("https://${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")
        var statuses: ArrayList<Status>? = null
        val newPosts = ArrayList<Post>()

        pixelfedAPI.timelinePublic(null, null, null, null, null)
            .enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        statuses = response.body() as ArrayList<Status>?
                        if(!statuses.isNullOrEmpty()) {
                            for (status in statuses!!) {
                                newPosts.add(Post(status))
                            }
                            setContent(newPosts, view)
                            Log.e("POSTS", newPosts.toString())
                        }

                    }
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })
    }
}
