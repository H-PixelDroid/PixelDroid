package com.h.pixeldroid.fragments.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Notification
import kotlinx.android.synthetic.main.fragment_feed.*

/**
 * A fragment representing a list of Items.
 */
class NotificationsFragment : FeedFragment<Notification, NotificationsRecyclerViewAdapter.ViewHolder>() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container)

        adapter = NotificationsRecyclerViewAdapter()
        list.adapter = adapter


        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        swipeRefreshLayout.setOnRefreshListener {
            val call = pixelfedAPI.notifications("Bearer $accessToken", min_id="1")
            doRequest(call)
        }
        val call = pixelfedAPI.notifications("Bearer $accessToken", min_id="1")
        doRequest(call)
    }


}