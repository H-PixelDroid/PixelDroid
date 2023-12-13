package org.pixeldroid.app.stories

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.StoryCarouselAddStoryBinding
import org.pixeldroid.app.databinding.StoryCarouselBinding
import org.pixeldroid.app.databinding.StoryCarouselItemBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.camera.CameraFragment
import org.pixeldroid.app.utils.api.objects.CarouselUserContainer
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.di.PixelfedAPIHolder


/**
 * Adapter to the show the a [RecyclerView] item for a [LoadState]
 */
class StoriesAdapter(val lifecycleScope: LifecycleCoroutineScope, val apiHolder: PixelfedAPIHolder) : RecyclerView.Adapter<StoryCarouselViewHolder>() {
    var carousel: StoryCarousel? = null

    /**
     * Whether to show stories or not.
     *
     * Changing this property will immediately notify the Adapter to change the item it's
     * presenting.
     */
    var showStories: Boolean = false
        set(newValue) {
            val oldValue = field

            if (oldValue && !newValue) {
                notifyItemRemoved(0)
            } else if (newValue && !oldValue) {
                notifyItemInserted(0)
            } else if (oldValue && newValue) {
                notifyItemChanged(0)
            }
            field = newValue

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryCarouselViewHolder {
        return StoryCarouselViewHolder.create(parent, ::noStories)
    }

    override fun onBindViewHolder(holder: StoryCarouselViewHolder, position: Int) {
        holder.bind(carousel)
    }

    override fun getItemViewType(position: Int): Int = 0

    override fun getItemCount(): Int = if (showStories) 1 else 0

    private fun noStories(){
        showStories = false
    }

    private fun gotStories(newCarousel: StoryCarousel) {
        carousel = newCarousel
        showStories = true
    }

    fun refreshStories(){
        lifecycleScope.launch {
            try{
                val api = apiHolder.api ?: apiHolder.setToCurrentUser()
                val carousel = api.carousel()

                if (carousel.nodes?.isEmpty() != true) {
                    // Pass carousel to adapter
                    gotStories(carousel)
                } else {
                    noStories()
                }
            } catch (exception: Exception){
                noStories()
            }
        }
    }

}

class StoryCarouselViewHolder(val binding: StoryCarouselBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(carousel: StoryCarousel?) {
        val adapter = StoriesListAdapter()
        binding.storyCarousel.adapter = adapter

        carousel?.let { adapter.initCarousel(it) }
    }

    companion object {
        fun create(parent: ViewGroup, noStories: () -> Unit): StoryCarouselViewHolder {
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