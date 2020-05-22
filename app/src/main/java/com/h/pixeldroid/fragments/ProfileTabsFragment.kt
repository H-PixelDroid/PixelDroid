package com.h.pixeldroid.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.feeds.FeedFragment
import com.h.pixeldroid.fragments.feeds.FeedsRecyclerViewAdapter
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Response

abstract class ProfileTabsFragment : FeedFragment<Status, ProfileTabsFragment.ProfilePostViewHolder>() {

    lateinit var picRequest: RequestBuilder<Drawable>
    private var columnCount = 3

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)// inflater.inflate(R.layout.fragment_profile_post, container, false)

        val db = DBUtils.initDB(requireContext())
        val user = db.userDao().getActiveUser()
        val domain = user?.instance_uri.orEmpty()

        pixelfedAPI = PixelfedAPI.create(domain)
        accessToken = user?.accessToken.orEmpty()

        //RequestBuilder that is re-used for every image
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        adapter = ProfilePostsRecyclerViewAdapter()
        list.adapter = adapter

        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Status> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Status> = RecyclerViewPreloader(
            Glide.with(this), adapter as ProfilePostsRecyclerViewAdapter,
            sizeProvider, 4
        )

        list.addOnScrollListener(preloader)

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

    internal open fun makeContent(): LiveData<PagedList<Status>> {
        fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return setPosts(adapter as ProfilePostsRecyclerViewAdapter, pixelfedAPI, accessToken,
                requestedLoadSize)
        }

        fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return setPosts(adapter as ProfilePostsRecyclerViewAdapter, pixelfedAPI, accessToken,
                requestedLoadSize)
        }

        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = FeedDataSource(::makeInitialCall, ::makeAfterCall)
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder(factory, config).build()
    }

    protected abstract fun setPosts(
        adapter: ProfilePostsRecyclerViewAdapter, pixelfedAPI: PixelfedAPI,
        accessToken: String?, requestedLoadSize: Int
    ): Call<List<Status>>

    protected fun handleAPIResponse(response: Response<List<Status>>) {
        if (response.code() == 200) {
            val posts = ArrayList<Status>()
            val statuses = response.body()!!
            for (status in statuses) {
                posts.add(status)
            }
            val size = posts.size
            posts.addAll(posts)
            adapter.notifyItemRangeInserted(size, posts.size)
        } else {
            Log.e("PROFILE POSTS: ", response.code().toString())
        }
    }


    inner class ProfilePostsRecyclerViewAdapter :
        FeedsRecyclerViewAdapter<Status, ProfilePostViewHolder>(),
        ListPreloader.PreloadModelProvider<Status> {

        private val posts: ArrayList<Status> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
            return ProfilePostViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
            val post = posts[position]

            if (post.sensitive)
                ImageConverter.setSquareImageFromURL(holder.postView, null, holder.postPreview)
            else
                ImageConverter.setSquareImageFromURL(
                    holder.postView,
                    post.getPostPreviewURL(),
                    holder.postPreview
                )

            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postPreview.context, PostActivity::class.java)
                intent.putExtra(Status.POST_TAG, post)
                holder.postPreview.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = posts.size
        override fun getPreloadItems(position: Int): MutableList<Status> {
            val post = getItem(position) ?: return mutableListOf()
            return mutableListOf(post)
        }

        override fun getPreloadRequestBuilder(item: Status): RequestBuilder<*>? {
            return picRequest.load(item.getPostPreviewURL())
        }
    }

    inner class ProfilePostViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
        val postPreview: ImageView = postView.findViewById(R.id.postPreview)
    }
}


