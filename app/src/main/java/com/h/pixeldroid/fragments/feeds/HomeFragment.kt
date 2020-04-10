package com.h.pixeldroid.fragments.feeds

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.android.material.textfield.TextInputEditText
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Context
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeFragment : FeedFragment<Status, HomeFragment.HomeRecyclerViewAdapter.ViewHolder>() {

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
        : FeedsRecyclerViewAdapter<Status, HomeRecyclerViewAdapter.ViewHolder>() {
        private val api = pixelfedAPI
        private val credential = "Bearer $accessToken"
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
            holder.profilePic.maxHeight = metrics.heightPixels
            holder.postPic.maxHeight = metrics.heightPixels

            //Setup the post layout
            post.setupPost(holder.postView, picRequest, holder.postPic, holder.profilePic)

            //Set initial favorite toggle value
            holder.isLiked = post.favourited

            //Set all of the different onclick listeners
            //Activate the liker
            holder.liker.setOnClickListener {
                if (holder.isLiked) {
                    api.unlikePost(credential, post.id).enqueue(object : Callback<Status> {
                        override fun onFailure(call: Call<Status>, t: Throwable) {
                            Log.e("UNLIKE ERROR", t.toString())
                        }

                        override fun onResponse(call: Call<Status>, response: Response<Status>) {
                            if(response.code() == 200) {
                                val resp = response.body()!!

                                //Update shown like count and internal like toggle
                                holder.nlikes.text = resp.getNLikes()
                                holder.isLiked = resp.favourited
                            } else {
                                Log.e("RESPOSE_CODE", response.code().toString())
                            }

                        }

                    })

                } else {
                    api.likePost(credential, post.id).enqueue(object : Callback<Status> {
                        override fun onFailure(call: Call<Status>, t: Throwable) {
                            Log.e("LIKE ERROR", t.toString())
                        }

                        override fun onResponse(call: Call<Status>, response: Response<Status>) {
                            if(response.code() == 200) {
                                val resp = response.body()!!

                                //Update shown like count and internal like toggle
                                holder.nlikes.text = resp.getNLikes()
                                holder.isLiked = resp.favourited
                            } else {
                                Log.e("RESPOSE_CODE", response.code().toString())
                            }
                        }

                    })

                }
            }

            //Show all comments of a post
            if (post.replies_count == 0) {
                holder.viewComment.text =  "No comments on this post..."
            } else {
                holder.viewComment.text =  "View all ${post.replies_count} comments..."
                holder.viewComment.setOnClickListener {
                    holder.viewComment.visibility = View.GONE
                    api.statusComments(post.id, credential).enqueue(object : Callback<com.h.pixeldroid.objects.Context> {
                        override fun onFailure(call: Call<com.h.pixeldroid.objects.Context>, t: Throwable) {
                            Log.e("COMMENT FETCH ERROR", t.toString())
                        }

                        override fun onResponse(
                            call: Call<com.h.pixeldroid.objects.Context>,
                            response: Response<Context>
                        ) {
                            if(response.code() == 200) {
                                val statuses = response.body()!!.descendants

                                //Create the new views for each comment
                                for (status in statuses) {
                                    post.addComment(
                                        context,
                                        holder.commentCont,
                                        status.account,
                                        status.content
                                    )
                                }
                            } else {
                                Log.e("COMMENT ERROR", "${response.code()} with body ${response.errorBody()}")
                            }
                        }
                    })
                }
            }

            //Toggle comment button
            holder.commenter.setOnClickListener {
                when(holder.commentIn.visibility) {
                    View.VISIBLE -> holder.commentIn.visibility = View.GONE
                    View.INVISIBLE -> holder.commentIn.visibility = View.VISIBLE
                    View.GONE -> holder.commentIn.visibility = View.VISIBLE
                }
            }

            //Activate commenter
            holder.submitCmnt.setOnClickListener {
                val textIn = holder.comment.text

                //Open text input
                if(textIn.isNullOrEmpty()) {
                    Toast.makeText(context,"Comment must not be empty!", Toast.LENGTH_SHORT).show()
                } else {
                    val nonNullText = textIn.toString()
                    api.postStatus(credential, nonNullText, post.id).enqueue(object :
                        Callback<Status> {
                        override fun onFailure(call: Call<Status>, t: Throwable) {
                            Log.e("COMMENT ERROR", t.toString())
                            Toast.makeText(context,"Comment error!", Toast.LENGTH_SHORT).show()
                        }

                        override fun onResponse(call: Call<Status>, response: Response<Status>) {
                            //Check that the received response code is valid
                            if(response.code() == 200) {
                                holder.commentIn.visibility = View.GONE
                                Toast.makeText(context,"Comment: \"$textIn\" posted!", Toast.LENGTH_SHORT).show()
                                Log.e("COMMENT SUCCESS", "posted: $textIn")
                            } else {
                                Log.e("ERROR_CODE", response.code().toString())
                            }
                        }
                    })
                }
            }
        }

        /**
         * Represents the posts that will be contained within the feed
         */
        inner class ViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
            val profilePic  : ImageView = postView.findViewById(R.id.profilePic)
            val postPic     : ImageView = postView.findViewById(R.id.postPicture)
            val username    : TextView  = postView.findViewById(R.id.username)
            val usernameDesc: TextView  = postView.findViewById(R.id.usernameDesc)
            val description : TextView  = postView.findViewById(R.id.description)
            val nlikes      : TextView  = postView.findViewById(R.id.nlikes)
            val nshares     : TextView  = postView.findViewById(R.id.nshares)
            val liker       : ImageView = postView.findViewById(R.id.liker)
            val submitCmnt  : ImageButton = postView.findViewById(R.id.submitComment)
            val commenter   : ImageView = postView.findViewById(R.id.commenter)
            val comment     : TextInputEditText = postView.findViewById(R.id.editComment)
            val commentCont : LinearLayout = postView.findViewById(R.id.commentContainer)
            val commentIn   : LinearLayout = postView.findViewById(R.id.commentIn)
            val viewComment : TextView = postView.findViewById(R.id.ViewComments)
            var isLiked : Boolean = false
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
