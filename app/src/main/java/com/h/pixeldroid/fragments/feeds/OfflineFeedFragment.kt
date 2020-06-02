package com.h.pixeldroid.fragments.feeds

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

import com.h.pixeldroid.R
import com.h.pixeldroid.db.PostDatabaseEntity
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.fragment_offline_feed.view.*
import kotlinx.android.synthetic.main.post_fragment.view.*


/**
 * A simple [Fragment] subclass.
 * Use the [OfflineFeedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfflineFeedFragment: Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    lateinit var picRequest: RequestBuilder<Drawable>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_offline_feed, container, false)
        val loadingAnimation = view.offline_feed_progress_bar
        loadingAnimation.visibility = View.VISIBLE
        picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))
        val db = DBUtils.initDB(requireContext())
        if (db.postDao().numberOfPosts() > 0) {
            val posts = db.postDao().getAll()
            viewManager = LinearLayoutManager(requireContext())
            viewAdapter = OfflinePostFeedAdapter(posts)
            loadingAnimation.visibility = View.GONE
            recyclerView = view.offline_feed_recyclerview.apply {
                visibility = View.VISIBLE
                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)
                // use a linear layout manager
                layoutManager = viewManager
                // specify an viewAdapter (see also next example)
                adapter = viewAdapter
            }
        } else {
            loadingAnimation.visibility = View.GONE
            view.offline_feed_placeholder_text.visibility = View.VISIBLE
        }
        view.offline_feed_progress_bar.visibility = View.GONE
        return view
    }

    inner class OfflinePostFeedAdapter(private val posts: List<PostDatabaseEntity>)
        : RecyclerView.Adapter<OfflinePostFeedAdapter.OfflinePostViewHolder>() {

        inner class OfflinePostViewHolder(private val postView: View)
            : RecyclerView.ViewHolder(postView) {
            val profilePic  : ImageView = postView.findViewById(R.id.profilePic)
            val postPic     : ImageView = postView.findViewById(R.id.postPicture)
            val username    : TextView = postView.findViewById(R.id.username)
            val description : TextView = postView.findViewById(R.id.description)
            val comment     : EditText = postView.findViewById(R.id.editComment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflinePostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.post_fragment, parent, false)
                .apply {
                    liker.visibility = View.GONE
                    nlikes.visibility = View.GONE
                    reblogger.visibility = View.GONE
                    nshares.visibility = View.GONE
                    commenter.visibility = View.GONE
                    postDomain.visibility = View.GONE
                }
            return OfflinePostViewHolder(view)
        }

        override fun onBindViewHolder(holder: OfflinePostViewHolder, position: Int) {
            val post = posts[position]
            val metrics = requireContext().resources.displayMetrics
            //Limit the height of the different images
            holder.profilePic.maxHeight = metrics.heightPixels
            holder.postPic.maxHeight = metrics.heightPixels
            //Setup username as a button that opens the profile
            holder.itemView.username.apply {
                text = post.account_name
                setTypeface(null, Typeface.BOLD)
            }
            //Convert the date to a readable string
            Status.ISO8601toDate(post.date, holder.itemView.postDate, false, requireContext())

            //Setup images
            ImageConverter.setRoundImageFromURL(
                holder.itemView,
                post.account_profile_picture,
                holder.profilePic
            )

            //Setup post pic only if there are media attachments
            if(!post.media_urls.isNullOrEmpty()) {
                // Standard layout
                holder.postPic.visibility = View.VISIBLE
                holder.itemView.postPager.visibility = View.GONE
                holder.itemView.postTabs.visibility = View.GONE
                holder.itemView.sensitiveWarning.visibility = View.GONE

                if(post.media_urls.size == 1) {
                    picRequest.load(post.media_urls[0]).into(holder.postPic)
                } else {
                    setupTabsLayout(rootView, request, homeFragment)
                }
                imagePopUpMenu(rootView, homeFragment.requireActivity())
            }

            //Set the special HTML text
            post.setDescription(holder.postView, api, credential)

        }

        override fun getItemCount(): Int {
            return posts.size
        }

    }
}

