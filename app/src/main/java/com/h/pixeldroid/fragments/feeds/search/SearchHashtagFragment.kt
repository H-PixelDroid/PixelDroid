package com.h.pixeldroid.fragments.feeds.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.R
import com.h.pixeldroid.fragments.feeds.AccountListFragment
import com.h.pixeldroid.fragments.feeds.FeedFragment
import com.h.pixeldroid.fragments.feeds.FeedsRecyclerViewAdapter
import com.h.pixeldroid.fragments.feeds.NotificationsFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Notification
import com.h.pixeldroid.objects.Results
import com.h.pixeldroid.objects.Tag
import kotlinx.android.synthetic.main.account_list_entry.view.*
import kotlinx.android.synthetic.main.fragment_tags.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchHashtagFragment: FeedFragment<Tag, SearchHashtagFragment.TagsRecyclerViewAdapter.ViewHolder>(){

    private lateinit var query: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        query = arguments?.getSerializable("searchFeed") as String

        adapter = TagsRecyclerViewAdapter()
        list.adapter = adapter

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content = makeContent()

        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })
    }

    inner class SearchTagsListDataSource: FeedDataSource(null, null){

        override fun newSource(): FeedDataSource {
            return SearchTagsListDataSource()
        }

        private fun makeInitialCall(requestedLoadSize: Int): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken",
                    limit="$requestedLoadSize", q=query,
                    type = Results.SearchType.hashtags)
        }
        private fun makeAfterCall(requestedLoadSize: Int, key: String): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken", offset=key.toInt(),
                    limit="$requestedLoadSize", q = query,
                    type = Results.SearchType.hashtags)
        }

        override fun getKey(item: Tag): String {
            val value = content.value
            val count = value?.loadedCount ?: 0
            return count.toString()
        }
        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<Tag>
        ) {
            enqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Tag>) {
            enqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }

        private fun enqueueCall(call: Call<Results>, callback: LoadCallback<Tag>){

            call.enqueue(object : Callback<Results> {
                override fun onResponse(call: Call<Results>, response: Response<Results>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!!.hashtags as ArrayList<Tag>
                        callback.onResult(notifications as List<Tag>)

                    } else{
                        Toast.makeText(context,"Something went wrong while loading", Toast.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<Results>, t: Throwable) {
                    Toast.makeText(context,"Could not get feed", Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }
    }

    private fun makeContent(): LiveData<PagedList<Tag>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory =
            FeedFragment<Tag, TagsRecyclerViewAdapter.ViewHolder>()
                .FeedDataSourceFactory(
                SearchTagsListDataSource()
            )
        return LivePagedListBuilder(factory, config).build()
    }

    inner class TagsRecyclerViewAdapter : FeedsRecyclerViewAdapter<Tag, TagsRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_tags, parent, false)
            context = view.context
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder : ViewHolder, position : Int) {
            val tag = getItem(position) ?: return

            @SuppressLint("SetTextI18n")
            holder.name.text = "#" + tag.name

            holder.mView.setOnClickListener { Log.e("Tag: ", tag.name) }
        }

        inner class ViewHolder(val mView : View) : RecyclerView.ViewHolder(mView) {
            val name : TextView = mView.tag_name
        }
    }
}