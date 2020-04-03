package com.h.pixeldroid.fragments.feeds

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.h.pixeldroid.BuildConfig
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.FeedContent
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_feed.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class FeedFragment<T: FeedContent, VH: RecyclerView.ViewHolder?>: Fragment() {

    lateinit var content: LiveData<PagedList<T>>
    lateinit var factory: FeedDataSourceFactory

    protected var accessToken: String? = null
    protected lateinit var pixelfedAPI: PixelfedAPI
    protected lateinit var preferences: SharedPreferences

    protected lateinit var list : RecyclerView
    protected lateinit var adapter : FeedsRecyclerViewAdapter<T, VH>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        list = swipeRefreshLayout.list
        // Set the adapter
        list.layoutManager = LinearLayoutManager(context)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = activity!!.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )

        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")

        swipeRefreshLayout.setOnRefreshListener {
            //by invalidating data, loadInitial will be called again
            factory.liveData.value!!.invalidate()
        }

    }

    inner class FeedDataSource(private val makeInitialCall: (Int) -> Call<List<T>>,
                               private val makeAfterCall: (Int, String) -> Call<List<T>>
    ): ItemKeyedDataSource<String, T>() {

        //We use the id as the key
        override fun getKey(item: T): String {
            return item.id
        }
        //This is called to initialize the list, so we want some of the latest statuses
        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<T>
        ) {
            enqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<T>) {
            enqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }

        override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<T>) {
            //do nothing here, it is expected to pull to refresh to load newer notifications
        }

        private fun enqueueCall(call: Call<List<T>>, callback: LoadCallback<T>){
            call.enqueue(object : Callback<List<T>> {
                override fun onResponse(call: Call<List<T>>, response: Response<List<T>>) {
                    if (response.code() == 200) {
                        val notifications = response.body()!! as ArrayList<T>
                        callback.onResult(notifications as List<T>)
                    } else{
                        Toast.makeText(context,"Something went wrong while loading", Toast.LENGTH_SHORT).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    progressBar.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<T>>, t: Throwable) {
                    Toast.makeText(context,"Could not get feed", Toast.LENGTH_SHORT).show()
                    Log.e("FeedFragment", t.toString())
                }
            })
        }
    }
    inner class FeedDataSourceFactory(
        private val makeInitialCall: (Int) -> Call<List<T>>,
        private val makeAfterCall: (Int, String) -> Call<List<T>>
    ): DataSource.Factory<String, T>() {
        lateinit var liveData: MutableLiveData<FeedDataSource>

        override fun create(): DataSource<String, T> {
            val dataSource = FeedDataSource(::makeInitialCall.get(), ::makeAfterCall.get())
            liveData = MutableLiveData()
            liveData.postValue(dataSource)
            return dataSource
        }


    }
}

abstract class FeedsRecyclerViewAdapter<T: FeedContent, VH : RecyclerView.ViewHolder?>: PagedListAdapter<T, VH>(
    object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem.id === newItem.id
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }
    }
), PreloadModelProvider<T> {

    protected lateinit var context: Context
}

