package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.ProfileFragment
import com.h.pixeldroid.fragments.ProfilePostsRecyclerViewAdapter
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    lateinit var profileFragment : ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set profile according to given account
        val account = intent.getSerializableExtra(ACCOUNT_TAG) as Account

        profileFragment = ProfileFragment()
        val arguments = Bundle()
        arguments.putSerializable("profileTag", account)
        profileFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.profileFragmentSingle, profileFragment).commit()
    }
}