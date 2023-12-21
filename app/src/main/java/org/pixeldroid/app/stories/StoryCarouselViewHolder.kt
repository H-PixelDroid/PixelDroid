package org.pixeldroid.app.stories

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.StoryCarouselBinding
import org.pixeldroid.app.databinding.StoryCarouselItemBinding
import org.pixeldroid.app.databinding.StoryCarouselSelfBinding
import org.pixeldroid.app.postCreation.camera.CameraActivity
import org.pixeldroid.app.postCreation.camera.CameraFragment
import org.pixeldroid.app.utils.api.objects.CarouselUserContainer
import org.pixeldroid.app.utils.api.objects.Story
import org.pixeldroid.app.utils.api.objects.StoryCarousel
import org.pixeldroid.app.utils.di.PixelfedAPIHolder


/**
 * Adapter that has either 1 or 0 items, to show stories widget or not
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

                // If there are stories from someone else or our stories to show, show them
                if (carousel.nodes?.isEmpty() == false || carousel.self?.nodes?.isEmpty() == false) {
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
        return if(viewType == R.layout.story_carousel_self){
            val v = StoryCarouselSelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            v.myStory.visibility =
                if (storyCarousel?.self?.nodes?.isEmpty() == false) View.VISIBLE
                else View.GONE

            AddViewHolder(v)
        }
        else {
            val v = StoryCarouselItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ViewHolder(v)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == 0) R.layout.story_carousel_self
        else R.layout.story_carousel_item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position > 0) {
            val carouselPosition = position - 1
            storyCarousel?.nodes?.get(carouselPosition)?.let { (holder as ViewHolder).bindItem(it) }
            holder.itemView.setOnClickListener {
                storyCarousel?.nodes?.get(carouselPosition)?.user?.id?.let { userId ->
                    val intent = Intent(holder.itemView.context, StoriesActivity::class.java)
                    intent.putExtra(StoriesActivity.STORY_CAROUSEL, storyCarousel)
                    intent.putExtra(StoriesActivity.STORY_CAROUSEL_USER_ID, userId)
                    holder.itemView.context.startActivity(intent)
                }
            }
        } else {
            storyCarousel?.self?.nodes?.let { (holder as? AddViewHolder)?.bindItem(it.filterNotNull()) }
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

    class AddViewHolder(private val itemBinding: StoryCarouselSelfBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bindItem(nodes: List<Story>) {
            itemBinding.addStory.setOnClickListener {
                val intent = Intent(itemView.context, CameraActivity::class.java)
                intent.putExtra(CameraFragment.CAMERA_ACTIVITY_STORY, true)
                itemView.context.startActivity(intent)
            }
            itemBinding.myStory.setOnClickListener {
                val intent = Intent(itemView.context, StoriesActivity::class.java)
                intent.putExtra(StoriesActivity.STORY_CAROUSEL_SELF, nodes.toTypedArray())
                itemView.context.startActivity(intent)
            }

            // Only show image on new Android versions, because the transformations need it and the
            // text is not legible without the transformations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Glide.with(itemBinding.root).load(nodes.firstOrNull()?.src).into(itemBinding.carouselImageView)
                val value = 70 * 255 / 100
                val darkFilterRenderEffect = PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
                val blurRenderEffect =
                    RenderEffect.createBlurEffect(
                        4f, 4f, Shader.TileMode.MIRROR
                    )
                val combinedEffect = RenderEffect.createColorFilterEffect(darkFilterRenderEffect, blurRenderEffect)
                itemBinding.carouselImageView.setRenderEffect(combinedEffect)
            }

        }
    }

    class ViewHolder(private val itemBinding: StoryCarouselItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bindItem(user: CarouselUserContainer) {
            Glide.with(itemBinding.root).load(user.nodes?.firstOrNull()?.src).into(itemBinding.carouselImageView)
            Glide.with(itemBinding.root).load(user.user?.avatar).circleCrop().into(itemBinding.storyAuthorProfilePicture)

            itemBinding.username.text = user.user?.username ?: "" //TODO check which one to use here!
        }
    }
}