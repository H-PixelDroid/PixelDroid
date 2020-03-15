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
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.*
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
    var notifications : List<Notification> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications_list, container, false)
        preferences = activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        list = view.findViewById(R.id.list)

        // Set the adapter
        list.layoutManager = LinearLayoutManager(context)
        adapter = NotificationsRecyclerViewAdapter()
        list.adapter = adapter
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")
        pixelfedAPI.notifications("Bearer $accessToken", min_id="1")
            .enqueue(object : Callback<List<Notification>> {
                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!! as ArrayList<Notification>
                        setContent(notifications)
                    } else{}
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })
    }
    fun setContent(notifications : ArrayList<Notification>) {
        this.notifications = notifications
        adapter.addNotifications(notifications)
    }

}