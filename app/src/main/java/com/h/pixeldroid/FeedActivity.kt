package com.h.pixeldroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_FRAG_TAG
import com.h.pixeldroid.objects.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedActivity : AppCompatActivity() {
    lateinit var feed : RecyclerView
    lateinit var adapter : FeedRecyclerViewAdapter
    var posts : List<Post> = ArrayList()

    fun setContent(newPosts : ArrayList<Post>) {
        feed = findViewById(R.id.feedList)
        feed?.setHasFixedSize(true)
        feed?.layoutManager = LinearLayoutManager(this)
        posts = newPosts
        adapter = FeedRecyclerViewAdapter(context = this)
        feed?.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        var statuses: ArrayList<Status>? = null
        val BASE_URL = "https://pixelfed.de/"

        val pixelfedAPI = PixelfedAPI.create(BASE_URL)

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
                            setContent(newPosts)
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
