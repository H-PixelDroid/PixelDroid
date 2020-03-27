package com.h.pixeldroid.fragments.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.h.pixeldroid.db.PostEntity
import com.h.pixeldroid.objects.Status
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : FeedFragment<Status, HomeRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container)
        adapter = HomeRecyclerViewAdapter()
        list.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setOnRefreshListener {
            val call = pixelfedAPI.timelineHome("Bearer $accessToken")
            doRequest(call)
        }
        val call = pixelfedAPI.timelineHome("Bearer $accessToken")
        doRequest(call)
    }
}
