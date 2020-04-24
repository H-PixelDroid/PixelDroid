package com.h.pixeldroid.fragments

import android.app.Activity
import android.app.SearchManager
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
import android.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.SearchActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Results
import com.h.pixeldroid.objects.Status
import kotlinx.android.synthetic.main.fragment_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * This fragment lets you search and use PixelFed's Discover feature
 */

class SearchDiscoverFragment : Fragment() {
    lateinit var api: PixelfedAPI
    private lateinit var preferences: SharedPreferences
    private lateinit var accessToken: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val button = view.findViewById<Button>(R.id.button)
        val search = view.findViewById<EditText>(R.id.search)
        button.setOnClickListener {
            val intent = Intent(context, SearchActivity::class.java)
            intent.putExtra("searchFeed", search.text.toString())
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        api = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "") ?: ""

    }
}
