package com.h.pixeldroid.fragments.feeds.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.fragments.feeds.AccountListFragment
import com.h.pixeldroid.fragments.feeds.FeedFragment
import com.h.pixeldroid.fragments.feeds.FeedsRecyclerViewAdapter
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Results
import com.h.pixeldroid.objects.Tag
import kotlinx.android.synthetic.main.fragment_tags.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchHashtagFragment: FeedFragment(){

    private lateinit var query: String
    private lateinit var content: LiveData<PagedList<Tag>>
    private lateinit var adapter : TagsRecyclerViewAdapter
    lateinit var factory: FeedDataSourceFactory<Int, Tag>


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

        swipeRefreshLayout.setOnRefreshListener {
            showError(show = false)

            //by invalidating data, loadInitial will be called again
            factory.liveData.value!!.invalidate()
        }
    }

    inner class SearchTagsListDataSource: FeedDataSource<Int, Tag>(){

        override fun newSource(): SearchTagsListDataSource {
            return SearchTagsListDataSource()
        }

        private fun searchMakeInitialCall(requestedLoadSize: Int): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken",
                    limit="$requestedLoadSize", q=query,
                    type = Results.SearchType.hashtags)
        }
        private fun searchMakeAfterCall(requestedLoadSize: Int, key: Int): Call<Results> {
            return pixelfedAPI
                .search("Bearer $accessToken", offset = key.toString(),
                    limit="$requestedLoadSize", q = query,
                    type = Results.SearchType.hashtags)
        }

        override fun getKey(item: Tag): Int {
            val value = content.value
            return value?.loadedCount ?: 0
        }
        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Tag>
        ) {
            searchEnqueueCall(searchMakeInitialCall(params.requestedLoadSize), callback)
        }

        override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Tag>) {
            searchEnqueueCall(searchMakeAfterCall(params.requestedLoadSize, params.key), callback)
        }

        private fun searchEnqueueCall(call: Call<Results>, callback: LoadCallback<Tag>){

            call.enqueue(object : Callback<Results> {
                override fun onResponse(call: Call<Results>, response: Response<Results>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!!.hashtags as ArrayList<Tag>
                        callback.onResult(notifications as List<Tag>)

                    } else{
                        showError()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<Results>, t: Throwable) {
                    showError(errorText = R.string.feed_failed)
                    Log.e("FeedFragment", t.toString())
                }
            })
        }

        override fun makeInitialCall(requestedLoadSize: Int): Call<List<Tag>> {
            throw NotImplementedError("Should not be called, reimplemented for Search fragment")
        }

        override fun makeAfterCall(requestedLoadSize: Int, key: Int): Call<List<Tag>> {
            throw NotImplementedError("Should not be called, reimplemented for Search fragment")
        }

        override fun enqueueCall(call: Call<List<Tag>>, callback: LoadCallback<Tag>) {
            throw NotImplementedError("Should not be called, reimplemented for Search fragment")
        }
    }

    private fun makeContent(): LiveData<PagedList<Tag>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory =
            FeedFragment()
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