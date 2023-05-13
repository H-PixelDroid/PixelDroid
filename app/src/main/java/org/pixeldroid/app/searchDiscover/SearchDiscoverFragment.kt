package org.pixeldroid.app.searchDiscover

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.carousel.CarouselLayoutManager
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.FragmentSearchBinding
import org.pixeldroid.app.databinding.StoryCarouselBinding
import org.pixeldroid.app.searchDiscover.TrendingActivity.Companion.TRENDING_TAG
import org.pixeldroid.app.searchDiscover.TrendingActivity.Companion.TrendingType
import org.pixeldroid.app.stories.StoriesActivity
import org.pixeldroid.app.stories.StoriesActivity.Companion.STORY_CAROUSEL
import org.pixeldroid.app.stories.StoriesActivity.Companion.STORY_CAROUSEL_USER_ID
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.CarouselUserContainer
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.bindingLifecycleAware


/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : BaseFragment() {

    private lateinit var api: PixelfedAPI

    var binding: FragmentSearchBinding by bindingLifecycleAware()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Configure the search widget (see https://developer.android.com/guide/topics/search/search-dialog#ConfiguringWidget)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.search.apply {
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            isSubmitButtonEnabled = true
        }

        val adapter = StoriesListAdapter(::onClickStory)
        binding.recyclerView2.adapter = adapter

        loadStories(adapter)

        binding.recyclerView2.layoutManager = CarouselLayoutManager()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = apiHolder.api ?: apiHolder.setToCurrentUser()

        binding.discoverCardView.setOnClickListener { onClickCardView(TrendingType.DISCOVER) }
        binding.trendingCardView.setOnClickListener { onClickCardView(TrendingType.POSTS) }
        binding.hashtagsCardView.setOnClickListener { onClickCardView(TrendingType.HASHTAGS) }
        binding.accountsCardView.setOnClickListener { onClickCardView(TrendingType.ACCOUNTS) }
    }

    private fun onClickCardView(type: TrendingType) {
        val intent = Intent(requireContext(), TrendingActivity::class.java)
        intent.putExtra(TRENDING_TAG, type)
        ContextCompat.startActivity(binding.root.context, intent, null)
    }

    private fun onClickStory(carousel: StoryCarousel, userId: String){
        val intent = Intent(requireContext(), StoriesActivity::class.java)
        intent.putExtra(STORY_CAROUSEL, carousel)
        intent.putExtra(STORY_CAROUSEL_USER_ID, userId)
        startActivity(intent)
    }

    private fun loadStories(adapter: StoriesListAdapter) {
        lifecycleScope.launch {
            try{
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                val carousel = api.carousel()
                adapter.initCarousel(carousel)
            } catch (exception: Exception){
                //TODO
            }
        }
    }

}

class StoriesListAdapter(private val listener: (StoryCarousel, String) -> Unit): RecyclerView.Adapter<StoriesListAdapter.ViewHolder>() {

    private var storyCarousel: StoryCarousel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = StoryCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        storyCarousel?.nodes?.get(position)?.let { holder.bindItem(it) }
        holder.itemView.setOnClickListener {
            storyCarousel?.let { carousel ->
                storyCarousel?.nodes?.get(position)?.user?.id?.let { userId ->
                    listener(
                        carousel,
                        userId
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return storyCarousel?.nodes?.size ?: 0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initCarousel(carousel: StoryCarousel){
        storyCarousel = carousel
        notifyDataSetChanged()
    }


    class ViewHolder(var itemBinding: StoryCarouselBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bindItem(user: CarouselUserContainer) {
            Glide.with(itemBinding.root).load(user.nodes?.firstOrNull()?.src).into(itemBinding.carouselImageView)
            Glide.with(itemBinding.root).load(user.user?.avatar).circleCrop().into(itemBinding.storyAuthorProfilePicture)

            itemBinding.username.text = user.user?.username ?: "" //TODO check which one to use here!
        }
    }
}