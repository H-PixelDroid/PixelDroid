package com.h.pixeldroid.fragments

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Notification
import kotlinx.android.synthetic.main.fragment_notifications_list.*
import kotlinx.android.synthetic.main.fragment_notifications_list.view.*
import kotlinx.android.synthetic.main.fragment_notifications_list.view.swipeRefreshLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A fragment representing a list of Items.
 */
class NotificationsFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var list : RecyclerView
    private lateinit var adapter : NotificationsRecyclerViewAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    var notifications : List<Notification> = ArrayList()

    private lateinit var pixelfedAPI: PixelfedAPI
    private var accessToken: String? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications_list, container, false)
        preferences = activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        list = swipeRefreshLayout.list

        // Set the adapter
        list.layoutManager = LinearLayoutManager(context)
        adapter = NotificationsRecyclerViewAdapter()
        list.adapter = adapter



        swipeRefreshLayout.setOnRefreshListener {
            doRequest()
        }

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        doRequest()
    }
    fun setContent(notifications : ArrayList<Notification>) {
        this.notifications = notifications
        adapter.initializeWith(notifications)
    }

    private fun doRequest(){
        val call = pixelfedAPI.notifications("Bearer $accessToken", min_id="1")
        call.enqueue(object : Callback<List<Notification>> {
            override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                if (response.code() == 200) {
                    val notifications = response.body()!! as ArrayList<Notification>
                    setContent(notifications)
                } else{
                    Toast.makeText(context,"Something went wrong while refreshing", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                Toast.makeText(context,"Could not get notifications", Toast.LENGTH_SHORT).show()
                Log.e("Notifications", t.toString())
            }
        })

    }

}