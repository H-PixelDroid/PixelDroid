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
import com.h.pixeldroid.objects.Notification
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_feed.view.*
import kotlinx.android.synthetic.main.fragment_feed.view.progressBar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


open class FeedFragment<T: FeedContent, VH: RecyclerView.ViewHolder?>: Fragment() {

    lateinit var content: LiveData<PagedList<T>>

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

    }

    internal fun enqueueCall(call: Call<List<T>>, callback: ItemKeyedDataSource.LoadCallback<T>){
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

    protected val feedContent: ArrayList<T> = arrayListOf()
    protected lateinit var context: Context

}



abstract class FeedDataSource: ItemKeyedDataSource<String, FeedContent>() {

    override fun getKey(item: FeedContent): String {
        return item.id
    }

}

abstract class FeedDataSourceFactory: DataSource.Factory<String, FeedContent>()