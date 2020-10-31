 package com.h.pixeldroid.fragments.feeds

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.h.pixeldroid.Pixeldroid
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.di.PixelfedAPIHolder
import com.h.pixeldroid.objects.FeedContent
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_feed.view.*
import retrofit2.Call
import javax.inject.Inject

open class FeedFragment: Fragment() {

    protected var accessToken: String? = null
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    protected lateinit var pixelfedAPI: PixelfedAPI

    protected lateinit var list : RecyclerView
    protected lateinit var swipeRefreshLayout: SwipeRefreshLayout
    internal lateinit var loadingIndicator: ProgressBar
    var user: UserDatabaseEntity? = null
    @Inject
    lateinit var db: AppDatabase



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        (requireActivity().application as Pixeldroid).getAppComponent().inject(this)

        //Initialize lateinit fields that are needed as soon as the view is created
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        loadingIndicator = view.findViewById(R.id.progressBar)
        list = swipeRefreshLayout.list
        list.layoutManager = LinearLayoutManager(context)
        user = db.userDao().getActiveUser()

        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        accessToken = user?.accessToken.orEmpty()

        return view
    }

    fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        if(show){
            errorLayout.visibility = VISIBLE
            progressBar.visibility = GONE
        } else {
            errorLayout.visibility = GONE
            progressBar.visibility = VISIBLE
        }
    }

    open inner class FeedDataSourceFactory<ObjectId, APIObject: FeedContent>(
        private val dataSource: FeedDataSource<ObjectId, APIObject>
    ): DataSource.Factory<ObjectId, APIObject>() {
        internal lateinit var liveData: MutableLiveData<FeedDataSource<ObjectId, APIObject>>

        override fun create(): DataSource<ObjectId, APIObject> {
            val dataSource = dataSource.newSource()
            liveData = MutableLiveData()
            liveData.postValue(dataSource)
            return dataSource
        }
    }
    abstract inner class FeedDataSource<ObjectId, APIObject: FeedContent>: ItemKeyedDataSource<ObjectId, APIObject>(){

        /**
         * Used in the initial call to initialize the list [loadInitial].
         * @param requestedLoadSize number of objects requested in a call
         * @return [Call] that gets the list of [APIObject]
         */
        abstract fun makeInitialCall(requestedLoadSize: Int): Call<List<APIObject>>

        /**
         * Used in the subsequent calls to get more objects.
         * @param requestedLoadSize number of objects requested in a call
         * @param key of the last object we already have
         * @return [Call] that gets the list of [APIObject]
         */
        abstract fun makeAfterCall(requestedLoadSize: Int, key: ObjectId): Call<List<APIObject>>

        /**
         * This is called to initialize the list, so we want some of the most recent objects.
         * @param params holds the requestedLoadSize
         * @param callback to call after network request completes
         */
        override fun loadInitial(
            params: LoadInitialParams<ObjectId>,
            callback: LoadInitialCallback<APIObject>
        ) {
            enqueueCall(makeInitialCall(params.requestedLoadSize), callback)
        }

        /**
         * This is called to when we get to the bottom of the loaded content, so we want objects
         * older than the given key (params.key).
         * @param params holds the requestedLoadSize
         * @param callback to call after network request completes
         */
        override fun loadAfter(params: LoadParams<ObjectId>, callback: LoadCallback<APIObject>) {
            enqueueCall(makeAfterCall(params.requestedLoadSize, params.key), callback)
        }

        /**
         * Do nothing here, it is expected to pull to refresh to load newer items
         */
        override fun loadBefore(params: LoadParams<ObjectId>, callback: LoadCallback<APIObject>) {}

        abstract fun enqueueCall(call: Call<List<APIObject>>, callback: LoadCallback<APIObject>)

        abstract fun newSource(): FeedDataSource<ObjectId, APIObject>

    }
}

abstract class FeedsRecyclerViewAdapter<T: FeedContent, VH : RecyclerView.ViewHolder?>: PagedListAdapter<T, VH>(
    object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem.id === newItem.id
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem.equals(newItem)
        }
    }
){
    protected lateinit var context: Context
}

