package com.h.pixeldroid.fragments

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.h.pixeldroid.Pixeldroid
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.di.PixelfedAPIHolder
import com.h.pixeldroid.objects.DiscoverPost
import com.h.pixeldroid.objects.DiscoverPosts
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.ImageConverter
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.color
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.android.synthetic.main.fragment_search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : Fragment() {
    private lateinit var api: PixelfedAPI
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : DiscoverRecyclerViewAdapter
    private lateinit var accessToken: String
    private lateinit var discoverProgressBar: ProgressBar
    private lateinit var discoverRefreshLayout: SwipeRefreshLayout
    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val search = view.findViewById<SearchView>(R.id.search)

        (requireActivity().application as Pixeldroid).getAppComponent().inject(this)


        //Configure the search widget (see https://developer.android.com/guide/topics/search/search-dialog#ConfiguringWidget)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        search.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        search.isSubmitButtonEnabled = true

        // Set posts RecyclerView as a grid with 3 columns
        recycler = view.findViewById(R.id.discoverList)
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = DiscoverRecyclerViewAdapter()
        recycler.adapter = adapter

        val discoverText = view.findViewById<TextView>(R.id.discoverText)

        discoverText.setCompoundDrawables(IconicsDrawable(requireContext(), GoogleMaterial.Icon.gmd_explore).apply {
            sizeDp = 24
            paddingDp = 20
            color = IconicsColor.colorRes(R.color.colorDrawing)
        }, null, null, null)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        accessToken = db.userDao().getActiveUser()?.accessToken.orEmpty()

        discoverProgressBar = view.findViewById(R.id.discoverProgressBar)
        discoverRefreshLayout = view.findViewById(R.id.discoverRefreshLayout)

        getDiscover()

        discoverRefreshLayout.setOnRefreshListener {
            getDiscover()
        }
    }

    fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        if(show){
            motionLayout.transitionToEnd()
        } else {
            motionLayout.transitionToStart()
        }
        discoverRefreshLayout.isRefreshing = false
        discoverProgressBar.visibility = View.GONE
    }


    private fun getDiscover() {

        api.discover("Bearer $accessToken")
            .enqueue(object : Callback<DiscoverPosts> {

                override fun onFailure(call: Call<DiscoverPosts>, t: Throwable) {
                    showError()
                    Log.e("SearchDiscoverFragment:", t.toString())
                }

                override fun onResponse(call: Call<DiscoverPosts>, response: Response<DiscoverPosts>) {
                    if(response.code() == 200) {
                        val discoverPosts = response.body()!!
                        adapter.addPosts(discoverPosts.posts)
                        showError(show = false)
                    }
                    else {
                        showError()
                    }
                }
            })
    }
    /**
     * [RecyclerView.Adapter] that can display a list of [DiscoverPost]s
     */
    class DiscoverRecyclerViewAdapter: RecyclerView.Adapter<DiscoverRecyclerViewAdapter.ViewHolder>() {
        private val posts: ArrayList<DiscoverPost> = ArrayList()

        fun addPosts(newPosts : List<DiscoverPost>) {
            posts.clear()
            posts.addAll(newPosts)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_profile_posts, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = posts[position]
            ImageConverter.setSquareImageFromURL(holder.postView, post.thumb, holder.postPreview)
            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postView.context, PostActivity::class.java)
                intent.putExtra(Status.DISCOVER_TAG, post)
                holder.postView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = posts.size

        inner class ViewHolder(val postView: View) : RecyclerView.ViewHolder(postView) {
            val postPreview: ImageView = postView.findViewById(R.id.postPreview)
        }
    }
}
