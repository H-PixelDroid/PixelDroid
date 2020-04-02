package com.h.pixeldroid.fragments.feeds

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : FeedFragment<Status, HomeFragment.HomeRecyclerViewAdapter.ViewHolder>() {

    lateinit var picRequest: RequestBuilder<Drawable>
    lateinit var factory: HomeDataSourceFactory


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = HomeDataSourceFactory()
        content = LivePagedListBuilder(factory, config).build()

        //RequestBuilder that is re-used for every image
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        adapter = HomeRecyclerViewAdapter()
        list.adapter = adapter

        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })

        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Status> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Status> = RecyclerViewPreloader(
            Glide.with(this), adapter, sizeProvider, 4
        )
        list.addOnScrollListener(preloader)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setOnRefreshListener {
            //by invalidating data, loadInitial will be called again
            factory.postLiveData.value!!.invalidate()
        }
    }

    /**
     * [RecyclerView.Adapter] that can display a list of Statuses
     */
    inner class HomeRecyclerViewAdapter: FeedsRecyclerViewAdapter<Status, HomeRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
            context = view.context
            return ViewHolder(view)
        }

        /**
         * Binds the different elements of the Post Model to the view holder
         */
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = getItem(position) ?: return
            val metrics = context.resources.displayMetrics
            //Limit the height of the different images
            holder.profilePic?.maxHeight = metrics.heightPixels
            holder.postPic.maxHeight = metrics.heightPixels

            //Set the two images
            ImageConverter.setRoundImageFromURL(
                holder.postView,
                post.getProfilePicUrl(),
                holder.profilePic!!
            )

            picRequest.load(post.getPostUrl()).into(holder.postPic)

            //Set the image back to a placeholder if the original is too big
            if(holder.postPic.height > metrics.heightPixels) {
                ImageConverter.setDefaultImage(holder.postView, holder.postPic)
            }

            //Set the the text views
            post.setupPost(holder.postView)
        }

        /**
         * Represents the posts that will be contained within the feed
         */
        inner class ViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
            val profilePic  : ImageView? = postView.findViewById(R.id.profilePic)
            val postPic     : ImageView = postView.findViewById(R.id.postPicture)
            val username    : TextView = postView.findViewById(R.id.username)
            val usernameDesc: TextView = postView.findViewById(R.id.usernameDesc)
            val description : TextView = postView.findViewById(R.id.description)
            val nlikes      : TextView = postView.findViewById(R.id.nlikes)
            val nshares     : TextView = postView.findViewById(R.id.nshares)
        }

        override fun getPreloadItems(position: Int): MutableList<Status> {
            val status = getItem(position) ?: return mutableListOf()
            return mutableListOf(status)
        }

        override fun getPreloadRequestBuilder(item: Status): RequestBuilder<*>? {
            return picRequest.load(item.getPostUrl())
        }
    }


    inner class HomeDataSource: ItemKeyedDataSource<String, Status>() {

        //We use the id as the key
        override fun getKey(item: Status): String {
            return item.id
        }
        //This is called to initialize the list, so we want some of the latest statuses
        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<Status>
        ) {
            val call = pixelfedAPI
                .timelineHome("Bearer $accessToken", limit="${params.requestedLoadSize}")
            enqueueCall(call, callback)
        }

        //This is called to when we get to the bottom of the loaded content, so we want statuses
        //older than the given key (params.key)
        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Status>) {
            val call = pixelfedAPI
                .timelineHome("Bearer $accessToken", max_id=params.key,
                    limit="${params.requestedLoadSize}")
            enqueueCall(call, callback)
        }

        override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<Status>) {
            //do nothing here, it is expected to pull to refresh to load newer content
        }

    }
    inner class HomeDataSourceFactory: DataSource.Factory<String, Status>() {

        lateinit var postLiveData: MutableLiveData<HomeDataSource>

        override fun create(): DataSource<String, Status> {
            val dataSource = HomeDataSource()
            postLiveData = MutableLiveData()
            postLiveData.postValue(dataSource)
            return dataSource
        }


    }
}
