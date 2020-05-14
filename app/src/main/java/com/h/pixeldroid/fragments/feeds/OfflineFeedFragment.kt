package com.h.pixeldroid.fragments.feeds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.h.pixeldroid.R
import kotlinx.android.synthetic.main.fragment_feed.view.feed_fragment_placeholder_text


/**
 * A simple [Fragment] subclass.
 * Use the [OfflineFeedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineFeedFragment: Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_offline_feed, container, false)
        view.feed_fragment_placeholder_text.visibility = View.VISIBLE
        return view
    }
}
