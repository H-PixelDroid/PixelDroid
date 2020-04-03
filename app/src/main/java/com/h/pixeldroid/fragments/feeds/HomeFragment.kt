package com.h.pixeldroid.fragments.feeds

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
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
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Context
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.fragment_home.*
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

        content = makeContent()

        //RequestBuilder that is re-used for every image
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        adapter = HomeRecyclerViewAdapter(pixelfedAPI, "Bearer $accessToken")
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
    inner class HomeRecyclerViewAdapter(private val api: PixelfedAPI, private val credential : String)
        : FeedsRecyclerViewAdapter<Status, HomeRecyclerViewAdapter.ViewHolder>() {

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

            //Limit the height of the different images
            holder.profilePic?.maxHeight = metrics.heightPixels
            holder.profilePic?.setOnClickListener{ Account.openProfile(context, post.account) }
            holder.postPic.maxHeight = metrics.heightPixels

            //Set the two images
            ImageConverter.setRoundImageFromURL(
                holder.postView,
                post.getProfilePicUrl(),
                holder.profilePic!!
            )
            picRequest.load(post.getPostUrl()).into(holder.postPic)

            //Set the the text views
            holder.username.text = post.getUsername()
            holder.username.setTypeface(null, Typeface.BOLD)
            holder.username.setOnClickListener{ Account.openProfile(context, post.account) }

            holder.usernameDesc.text = post.getUsername()
            holder.usernameDesc.setTypeface(null, Typeface.BOLD)

            holder.description.text = post.getDescription()

            holder.nlikes.text = post.getNLikes()
            holder.nlikes.setTypeface(null, Typeface.BOLD)

            holder.nshares.text = post.getNShares()
            holder.nshares.setTypeface(null, Typeface.BOLD)

            Log.e("Credential", credential)
            Log.e("POST_ID", post.id)

            //Activate the liker
            holder.liker.setOnClickListener {
                if (post.favourited) {
                    api.unlikePost(credential, post.id).enqueue(object : Callback<Status> {
                        override fun onFailure(call: Call<Status>, t: Throwable) {
                            Log.e("UNLIKE ERROR", t.toString())
                        }

                        override fun onResponse(call: Call<Status>, response: Response<Status>) {
                            if(response.code() == 200) {
                                val resp = response.body()!!
                                holder.nlikes.text = resp.getNLikes()
                                Log.e("POST", "unLiked")
                                Log.e("isLIKE", post.favourited.toString())

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
                                holder.nlikes.text = resp.getNLikes()
                                Log.e("POST", "liked")
                                Log.e("isLIKE", post.favourited.toString())
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
                    api.statusComments(post.id, credential).enqueue(object : Callback<Context> {
                        override fun onFailure(call: Call<com.h.pixeldroid.objects.Context>, t: Throwable) {
                            Log.e("COMMENT FETCH ERROR", t.toString())
                        }

                        override fun onResponse(
                            call: Call<com.h.pixeldroid.objects.Context>,
                            response: Response<Context>
                        ) {
                            if(response.code() == 200) {
                                val statuses = response.body()!!.descendants
                                Log.e("COMMENT STATUS", statuses.toString())

                                //Create the new views for each comment
                                for (status in statuses) {
                                    //Create UI views
                                    val container = CardView(context)
                                    val layout = LinearLayout(context)
                                    val comment = TextView(context)
                                    val user = TextView(context)

                                    //Set layout constraints and content
                                    container.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                    container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    layout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                    layout.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    user.text = status.account.username
                                    user.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                    user.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    (user.layoutParams as LinearLayout.LayoutParams).weight = 8f
                                    user.typeface = Typeface.DEFAULT_BOLD
                                    comment.text = status.content
                                    comment.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                    comment.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    (comment.layoutParams as LinearLayout.LayoutParams).weight = 2f

                                    //Create comment view hierarchy
                                    layout.addView(user)
                                    layout.addView(comment)
                                    container.addView(layout)
                                }
                            } else {
                                Log.e("COMMENT ERROR", "${response.code()} with body ${response.errorBody()}")
                            }
                        }
                    })
                }
            }

            //Set comment initial visibility
            holder.commentIn.visibility = View.GONE

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
                Log.e("ID", post.id)
                Log.e("ACCESS_TOKEN", credential)
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
            val profilePic  : ImageView? = postView.findViewById(R.id.profilePic)
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
