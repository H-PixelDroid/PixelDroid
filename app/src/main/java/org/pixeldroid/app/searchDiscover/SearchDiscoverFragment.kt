package org.pixeldroid.app.searchDiscover

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentSearchBinding
import org.pixeldroid.app.profile.ProfilePostViewHolder
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.ImageConverter
import org.pixeldroid.app.utils.bindingLifecycleAware
import retrofit2.HttpException
import java.io.IOException

/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : BaseFragment() {
    private lateinit var api: PixelfedAPI
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : DiscoverRecyclerViewAdapter

    var binding: FragmentSearchBinding by bindingLifecycleAware()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Configure the search widget (see https://developer.android.com/guide/topics/search/search-dialog#ConfiguringWidget)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.search.apply {
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            isSubmitButtonEnabled = true
        }

        // Set posts RecyclerView as a grid with 3 columns
        recycler = binding.discoverList
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = DiscoverRecyclerViewAdapter()
        recycler.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = apiHolder.api ?: apiHolder.setToCurrentUser()

        getDiscover()

        binding.discoverRefreshLayout.setOnRefreshListener {
            getDiscover()
        }
    }

    fun showError(@StringRes errorText: Int = R.string.loading_toast, show: Boolean = true){
        binding.motionLayout.apply {
            if(show){
                transitionToEnd()
            } else {
                transitionToStart()
            }
        }
        binding.discoverRefreshLayout.isRefreshing = false
        binding.discoverProgressBar.visibility = View.GONE
    }


    private fun getDiscover() {
        lifecycleScope.launchWhenCreated {
            try {
                val discoverPosts = api.discover()
                adapter.addPosts(discoverPosts.posts)
                binding.discoverNoInfiniteLoad.visibility = View.VISIBLE
                showError(show = false)
            } catch (exception: IOException) {
                showError()
            } catch (exception: HttpException) {
                showError()
            }
        }
    }

    /**
     * [RecyclerView.Adapter] that can display a list of [Status]s' thumbnails for the discover view
     */
    class DiscoverRecyclerViewAdapter: RecyclerView.Adapter<ProfilePostViewHolder>() {
        private val posts: ArrayList<Status> = ArrayList()

        fun addPosts(newPosts : List<Status>) {
            posts.clear()
            posts.addAll(newPosts)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_profile_posts, parent, false)
            return ProfilePostViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
            val post = posts[position]
            if(post.media_attachments?.size ?: 0 > 1) {
                holder.albumIcon.visibility = View.VISIBLE
            } else {
                holder.albumIcon.visibility = View.GONE
            }
            ImageConverter.setSquareImageFromURL(holder.postView, post.media_attachments?.firstOrNull()?.preview_url, holder.postPreview, post.media_attachments?.firstOrNull()?.blurhash)
            holder.postPreview.setOnClickListener {
                val intent = Intent(holder.postView.context, PostActivity::class.java)
                intent.putExtra(Status.POST_TAG, post)
                holder.postView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = posts.size
    }
}
