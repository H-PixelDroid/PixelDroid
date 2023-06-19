package org.pixeldroid.app.stories

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.StoryCarouselAddStoryBinding
import org.pixeldroid.app.databinding.StoryCarouselBinding
import org.pixeldroid.app.databinding.StoryCarouselItemBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.camera.CameraFragment
import org.pixeldroid.app.postCreation.carousel.dpToPx
import org.pixeldroid.app.utils.api.objects.CarouselUserContainer
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.di.PixelfedAPIHolder

class StoryCarouselViewHolder(val binding: StoryCarouselBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        pixelfedAPI: PixelfedAPIHolder,
        lifecycleScope: LifecycleCoroutineScope,
        itemView: View
    ) {
        val adapter = StoriesListAdapter()
        binding.storyCarousel.adapter = adapter

        loadStories(adapter, lifecycleScope, pixelfedAPI, itemView)
    }

    private fun loadStories(
        adapter: StoriesListAdapter,
        lifecycleScope: LifecycleCoroutineScope,
        apiHolder: PixelfedAPIHolder,
        itemView: View
    ) {
        lifecycleScope.launch {
            try{
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                val carousel = api.carousel()

                if (carousel.nodes?.isEmpty() != true) {
                    itemView.visibility = View.VISIBLE
                    itemView.layoutParams.height = 200.dpToPx(binding.root.context)
                    itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

                    adapter.initCarousel(carousel)
                }

            } catch (exception: Exception){
                //TODO
            }
        }
    }

    companion object {
        fun create(parent: ViewGroup): StoryCarouselViewHolder {
            val itemBinding = StoryCarouselBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return StoryCarouselViewHolder(itemBinding)
        }
    }
}


class StoriesListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var storyCarousel: StoryCarousel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == R.layout.story_carousel_add_story){
            val v = StoryCarouselAddStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AddViewHolder(v)
        }
        else {
            val v = StoryCarouselItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ViewHolder(v)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == 0) R.layout.story_carousel_add_story
        else R.layout.story_carousel_item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position > 0) {
            val carouselPosition = position - 1
            storyCarousel?.nodes?.get(carouselPosition)?.let { (holder as ViewHolder).bindItem(it) }
            holder.itemView.setOnClickListener {
                storyCarousel?.let { carousel ->
                    storyCarousel?.nodes?.get(carouselPosition)?.user?.id?.let { userId ->
                        val intent = Intent(holder.itemView.context, StoriesActivity::class.java)
                        intent.putExtra(StoriesActivity.STORY_CAROUSEL, carousel)
                        intent.putExtra(StoriesActivity.STORY_CAROUSEL_USER_ID, userId)
                        holder.itemView.context.startActivity(intent)
                    }
                }
            }
        } else {
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, CameraActivity::class.java)
                intent.putExtra(CameraFragment.CAMERA_ACTIVITY_STORY, true)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        // If the storyCarousel is not set, the carousel is not shown, so itemCount of 0
        return (storyCarousel?.nodes?.size?.plus(1)) ?: 0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initCarousel(carousel: StoryCarousel){
        storyCarousel = carousel
        notifyDataSetChanged()
    }

    class AddViewHolder(itemBinding: StoryCarouselAddStoryBinding) : RecyclerView.ViewHolder(itemBinding.root)

    class ViewHolder(private val itemBinding: StoryCarouselItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bindItem(user: CarouselUserContainer) {
            Glide.with(itemBinding.root).load(user.nodes?.firstOrNull()?.src).into(itemBinding.carouselImageView)
            Glide.with(itemBinding.root).load(user.user?.avatar).circleCrop().into(itemBinding.storyAuthorProfilePicture)

            itemBinding.username.text = user.user?.username ?: "" //TODO check which one to use here!
        }
    }
}