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
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import retrofit2.Call

open class PostsFeedFragment : FeedFragment<Status, PostViewHolder>() {

    lateinit var picRequest: RequestBuilder<Drawable>
    lateinit var domain : String
    private var user: UserDatabaseEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val db = DBUtils.initDB(requireContext())
        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        //RequestBuilder that is re-used for every image
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        adapter = PostsFeedRecyclerViewAdapter(this)
        list.adapter = adapter


        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Status> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Status> = RecyclerViewPreloader(
            Glide.with(this), adapter as PostsFeedFragment.PostsFeedRecyclerViewAdapter, sizeProvider, 4
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
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", limit="$requestedLoadSize")
        }
        fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Status>> {
            return pixelfedAPI
                .timelineHome("Bearer $accessToken", max_id=key,
                    limit="$requestedLoadSize")
        }
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = FeedDataSource(::makeInitialCall, ::makeAfterCall)
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder(factory, config).build()
    }

    /**
     * [RecyclerView.Adapter] that can display a list of Statuses
     */
    inner class PostsFeedRecyclerViewAdapter(private val postsFeedFragment: PostsFeedFragment)
        : FeedsRecyclerViewAdapter<Status, PostViewHolder>(),
        ListPreloader.PreloadModelProvider<Status> {
        private val api = pixelfedAPI
        private val credential = "Bearer $accessToken"
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
            context = view.context
            return PostViewHolder(view, context)
        }

        /**
         * Binds the different elements of the Post Model to the view holder
         */
        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = getItem(position) ?: return
            val metrics = context.resources.displayMetrics
            //Limit the height of the different images
            holder.profilePic.maxHeight = metrics.heightPixels
            holder.postPic.maxHeight = metrics.heightPixels

            //Setup the post layout
            post.setupPost(holder.postView, picRequest, this@PostsFeedFragment, domain, false)

            //Set the special HTML text
            post.setDescription(holder.postView, api, credential)

            //Activate liker
            post.activateLiker(holder, api, credential, post.favourited)

            //Activate double tap liking
            post.activateDoubleTapLiker(holder, api, credential)

            //Show comments
            post.showComments(holder, api, credential)

            //Activate Commenter
            post.activateCommenter(holder, api, credential)

            //Activate Reblogger
            post.activateReblogger(holder, api ,credential, post.reblogged)
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
class PostViewHolder(val postView: View, val context: android.content.Context) : RecyclerView.ViewHolder(postView) {
    val profilePic  : ImageView = postView.findViewById(R.id.profilePic)
    val postPic     : ImageView = postView.findViewById(R.id.postPicture)
    val username    : TextView  = postView.findViewById(R.id.username)
    val usernameDesc: TextView  = postView.findViewById(R.id.usernameDesc)
    val description : TextView  = postView.findViewById(R.id.description)
    val nlikes      : TextView  = postView.findViewById(R.id.nlikes)
    val nshares     : TextView  = postView.findViewById(R.id.nshares)

    //Spark buttons
    val liker       : SparkButton = postView.findViewById(R.id.liker)
    val reblogger   : SparkButton = postView.findViewById(R.id.reblogger)

    val submitCmnt  : ImageButton = postView.findViewById(R.id.submitComment)
    val commenter   : ImageView = postView.findViewById(R.id.commenter)
    val comment     : EditText = postView.findViewById(R.id.editComment)
    val commentCont : LinearLayout = postView.findViewById(R.id.commentContainer)
    val commentIn   : LinearLayout = postView.findViewById(R.id.commentIn)
    val viewComment : TextView = postView.findViewById(R.id.ViewComments)
    val postDate    : TextView = postView.findViewById(R.id.postDate)
    val postDomain  : TextView = postView.findViewById(R.id.postDomain)
    val sensitiveW  : TextView = postView.findViewById(R.id.sensitiveWarning)
}
