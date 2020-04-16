package com.h.pixeldroid.fragments.feeds

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import at.connyduck.sparkbutton.SparkButton
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import retrofit2.Call


class HomeFragment : FeedFragment<Status, ViewHolder>() {

    lateinit var picRequest: RequestBuilder<Drawable>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        //RequestBuilder that is re-used for every image
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        adapter = HomeRecyclerViewAdapter()
        list.adapter = adapter


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
        content = makeContent()
        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })
    }

    private fun makeContent(): LiveData<PagedList<Status>> {
        fun makeInitialCall(requestedLoadSize: Int): Call<List<Status>> {
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", limit="$requestedLoadSize")
        }
        fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", max_id=key,
                    limit="$requestedLoadSize")
        }
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        factory = FeedDataSourceFactory(::makeInitialCall, ::makeAfterCall)
        return LivePagedListBuilder(factory, config).build()
    }

    /**
     * [RecyclerView.Adapter] that can display a list of Statuses
     */
    inner class HomeRecyclerViewAdapter()
        : FeedsRecyclerViewAdapter<Status, ViewHolder>() {
        private val api = pixelfedAPI
        private val credential = "Bearer $accessToken"
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
            context = view.context
            return ViewHolder(view, context)
        }

        /**
         * Binds the different elements of the Post Model to the view holder
         */
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = getItem(position) ?: return
            val metrics = context.resources.displayMetrics
            //Limit the height of the different images
            holder.profilePic.maxHeight = metrics.heightPixels
            holder.postPic.maxHeight = metrics.heightPixels

            //Setup the post layout
            post.setupPost(holder.postView, picRequest, holder.postPic, holder.profilePic)

            //Activate liker
            post.activateLiker(holder, api, credential, post.favourited)

            //Show comments
            post.showComments(holder, api, credential)

            //Activate Commenter
            post.activateCommenter(holder, api, credential)
        }

        override fun getPreloadItems(position: Int): MutableList<Status> {
            val status = getItem(position) ?: return mutableListOf()
            return mutableListOf(status)
        }

        override fun getPreloadRequestBuilder(item: Status): RequestBuilder<*>? {
            return picRequest.load(item.getPostUrl())
        }
    }
}

/**
 * Represents the posts that will be contained within the feed
 */
class ViewHolder(val postView: View, val context: android.content.Context) : RecyclerView.ViewHolder(postView) {
    val profilePic  : ImageView = postView.findViewById(R.id.profilePic)
    val postPic     : ImageView = postView.findViewById(R.id.postPicture)
    val username    : TextView  = postView.findViewById(R.id.username)
    val usernameDesc: TextView  = postView.findViewById(R.id.usernameDesc)
    val description : TextView  = postView.findViewById(R.id.description)
    val nlikes      : TextView  = postView.findViewById(R.id.nlikes)
    val nshares     : TextView  = postView.findViewById(R.id.nshares)
    val liker       : SparkButton = postView.findViewById(R.id.liker)
    val submitCmnt  : ImageButton = postView.findViewById(R.id.submitComment)
    val commenter   : ImageView = postView.findViewById(R.id.commenter)
    val comment     : EditText = postView.findViewById(R.id.editComment)
    val commentCont : LinearLayout = postView.findViewById(R.id.commentContainer)
    val commentIn   : LinearLayout = postView.findViewById(R.id.commentIn)
    val viewComment : TextView = postView.findViewById(R.id.ViewComments)
}
