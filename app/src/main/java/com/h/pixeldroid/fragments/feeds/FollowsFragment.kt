package com.h.pixeldroid.fragments.feeds

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWING_TAG
import kotlinx.android.synthetic.main.fragment_follows.view.*
import retrofit2.Call

class FollowsFragment : FeedFragment<Account, FollowsFragment.FollowsRecyclerViewAdapter.ViewHolder>() {
    lateinit var profilePicRequest: RequestBuilder<Drawable>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        //RequestBuilder that is re-used for every image
        profilePicRequest = Glide.with(this)
            .asDrawable().apply(RequestOptions().circleCrop())
            .placeholder(R.drawable.ic_default_user)

        adapter = FollowsRecyclerViewAdapter()
        list.adapter = adapter

        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Account> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Account> = RecyclerViewPreloader(
            Glide.with(this), adapter, sizeProvider, 4
        )
        list.addOnScrollListener(preloader)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getSerializable(ACCOUNT_ID_TAG) as String
        val following = arguments?.getSerializable(FOLLOWING_TAG) as Boolean
        content = makeContent(id, following)

        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })
    }

    private fun makeContent(id : String, following : Boolean) : LiveData<PagedList<Account>> {
        fun makeInitialCall(requestedLoadSize: Int): Call<List<Account>> {
            if(following) {
                return pixelfedAPI.followers(id, "Bearer $accessToken", limit = requestedLoadSize)
            } else {
                return pixelfedAPI.following(id, "Bearer $accessToken", limit = requestedLoadSize)
            }
        }
        fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Account>> {
            if(following) {
                return pixelfedAPI.followers(id, "Bearer $accessToken", max_id = key, limit=requestedLoadSize)
            } else {
                return pixelfedAPI.following(id, "Bearer $accessToken", max_id = key, limit=requestedLoadSize)
            }
        }
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = FeedDataSourceFactory(::makeInitialCall, ::makeAfterCall)
        return LivePagedListBuilder(factory, config).build()
    }

    inner class FollowsRecyclerViewAdapter : FeedsRecyclerViewAdapter<Account,FollowsRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_follows, parent, false)
            context = view.context
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder : ViewHolder, position : Int) {
            val account = getItem(position) ?: return
            profilePicRequest.load(account.avatar_static).into(holder.avatar)

            holder.username.text = account.username

            holder.mView.setOnClickListener { account.openProfile(context) }
        }

        inner class ViewHolder(val mView : View) : RecyclerView.ViewHolder(mView) {
            val avatar : ImageView = mView.follows_avatar
            val username : TextView = mView.follows_username
        }

        override fun getPreloadItems(position : Int) : MutableList<Account> {
            val account = getItem(position) ?: return mutableListOf()
            return mutableListOf(account)
        }

        override fun getPreloadRequestBuilder(item : Account) : RequestBuilder<*>? {
            return profilePicRequest.load(item.avatar_static)
        }
    }
}