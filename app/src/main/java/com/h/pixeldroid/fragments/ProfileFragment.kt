package com.h.pixeldroid.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.FollowersActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.objects.Status
import kotlinx.android.synthetic.main.profile_fragment.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ProfileFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var adapter : ProfilePostsRecyclerViewAdapter
    private lateinit var recycler : RecyclerView
    private var account: Account? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferences = this.requireActivity().getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        account = arguments?.getSerializable(ACCOUNT_TAG) as Account?
        val view = inflater.inflate(R.layout.profile_fragment, container, false)

        if(account == null) {
            // Edit button redirects to Pixelfed's "edit account" page
            val editButton: Button = view.findViewById(R.id.editButton)
            editButton.visibility = View.VISIBLE
            editButton.setOnClickListener{ onClickEditButton() }
        }

        // Set RecyclerView as a grid with 3 columns
        recycler = view.findViewById(R.id.profilePostsRecyclerView)
        recycler.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfilePostsRecyclerViewAdapter(requireContext())
        recycler.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")

        if(account == null) {
            pixelfedAPI.verifyCredentials("Bearer $accessToken")
                .enqueue(object : Callback<Account> {
                    override fun onResponse(call: Call<Account>, response: Response<Account>) {
                        if (response.code() == 200) {
                            val account = response.body()!!

                            account.setContent(view)

                            // Populate profile page with user's posts
                            setPosts(account)
                        }
                    }

                    override fun onFailure(call: Call<Account>, t: Throwable) {
                        Log.e("ProfileFragment:", t.toString())
                    }
                })
        } else {
            account!!.activateFollow(view, requireContext(), pixelfedAPI, accessToken)
            account!!.setContent(view)
            setPosts(account!!)
            
            // Open followers list
            view.nbFollowersTextView.setOnClickListener{ onClickFollowers() }
        }
    }
    // Populate profile page with user's posts
    private fun setPosts(account: Account) {
        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        val accessToken = preferences.getString("accessToken", "")

        pixelfedAPI.accountPosts("Bearer $accessToken", account_id = account.id).enqueue(object :
            Callback<List<Status>> {
            override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                Log.e("ProfileFragment.Posts:", t.toString())
            }

            override fun onResponse(
                call: Call<List<Status>>,
                response: Response<List<Status>>
            ) {
                if(response.code() == 200) {
                    val posts = ArrayList<Status>()
                    val statuses = response.body()!!
                    for(status in statuses) {
                        posts.add(status)
                    }
                    adapter.addPosts(posts)
                }
            }
        })
    }
    private fun onClickEditButton() {
        val url = "${preferences.getString("domain", "")}/settings/home"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if(activity != null && browserIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(browserIntent)
        } else {
            val text = "Cannot open this link"
            Log.e("ProfileFragment", text)
        }
    }

    private fun onClickFollowers() {
        val intent = Intent(context, FollowersActivity::class.java)
        intent.putExtra(ACCOUNT_ID_TAG, account!!.id)
        ContextCompat.startActivity(requireContext(), intent, null)
    }
}
