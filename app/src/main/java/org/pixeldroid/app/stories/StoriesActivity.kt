package org.pixeldroid.app.stories

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.ActivityStoriesBinding
import org.pixeldroid.app.posts.setTextViewFromISO8601
import org.pixeldroid.app.utils.BaseThemedWithoutBarActivity


class StoriesActivity: BaseThemedWithoutBarActivity() {

    private lateinit var binding: ActivityStoriesBinding

    private lateinit var model: StoriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val _model: StoriesViewModel by viewModels {
            StoriesViewModelFactory(application)
        }
        model = _model

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    binding.pause.isSelected = uiState.paused

                    uiState.age?.let { setTextViewFromISO8601(it, binding.storyAge, false) }

                    uiState.profilePicture?.let {
                        Glide.with(binding.storyAuthorProfilePicture)
                            .load(it)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.storyAuthorProfilePicture)
                    }

                    binding.storyAuthor.text = uiState.username

                    uiState.imageList.getOrNull(uiState.currentImage)?.let {
                        Glide.with(binding.storyImage)
                            .load(it)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean,
                                ): Boolean = false

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    m: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean,
                                ): Boolean {
                                    model.imageLoaded()
                                    return false
                                }
                            })
                            .into(binding.storyImage)
                    }
                }
            }
        }

        model.count.observe(this) { state ->
            // Render state in UI
            model.uiState.value.durationList.getOrNull(model.uiState.value.currentImage)?.let {
                val percent = 100 - ((state/it.toFloat())*100).toInt()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.progressBarStory.setProgress(percent, true)
                } else {
                    binding.progressBarStory.progress = percent
                }
            }
        }

        binding.pause.setOnClickListener {
                //Set the button's appearance
                it.isSelected = !it.isSelected
                if (it.isSelected) {
                    //Handle selected state change
                } else {
                    //Handle de-select state change
                }
            }

        binding.storyImage.setOnClickListener {
            model.pause()
        }
    }
}