package com.h.pixeldroid.fragments.feeds

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_feed.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class FeedFragment<T, VH: RecyclerView.ViewHolder?>: Fragment() {

    var content : List<T> = ArrayList()

    protected var accessToken: String? = null
    protected lateinit var pixelfedAPI: PixelfedAPI
    protected lateinit var preferences: SharedPreferences

    protected lateinit var list : RecyclerView
    protected lateinit var adapter : FeedsRecyclerViewAdapter<T, VH>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    protected fun doRequest(call: Call<List<T>>){
        call.enqueue(object : Callback<List<T>> {
            override fun onResponse(call: Call<List<T>>, response: Response<List<T>>) {
                if (response.code() == 200) {
                    val notifications = response.body()!! as ArrayList<T>
                    setContent(notifications)
                } else{
                    Toast.makeText(context,"Something went wrong while loading", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = View.GONE
            }

            override fun onFailure(call: Call<List<T>>, t: Throwable) {
                Toast.makeText(context,"Could not get feed", Toast.LENGTH_SHORT).show()
                Log.e("FeedFragment", t.toString())
            }
        })

    }
    protected fun setContent(content : ArrayList<T>) {
        this.content = content
        adapter.initializeWith(content)
    }


    protected fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        list = swipeRefreshLayout.list
        // Set the adapter
        list.layoutManager = LinearLayoutManager(context)

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )

        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")

    }
}

abstract class FeedsRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder?>: RecyclerView.Adapter<VH>() {

    protected val feedContent: ArrayList<T> = arrayListOf()
    protected lateinit var context: Context

    override fun getItemCount(): Int = feedContent.size

    open fun initializeWith(content: List<T>){
        feedContent.clear()
        feedContent.addAll(content)
        notifyDataSetChanged()
    }
}