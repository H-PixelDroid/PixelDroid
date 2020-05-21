package com.h.pixeldroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.adapters.ProfilePostsRecyclerViewAdapter


/**
<<<<<<< HEAD:app/src/main/java/com/h/pixeldroid/fragments/ProfilePostGridFragment.kt
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ProfilePostGridFragment.OnListFragmentInteractionListener] interface.
 */
class ProfilePostGridFragment : Fragment() {

    private var columnCount = 3

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_posts_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = ProfilePostsRecyclerViewAdapter(requireContext())
            }
        }
        return view
    }
}
