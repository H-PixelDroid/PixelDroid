package org.pixeldroid.app.stories

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityStoriesBinding
import org.pixeldroid.app.posts.setTextViewFromISO8601
import org.pixeldroid.app.utils.BaseThemedWithoutBarActivity
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.StoryCarousel


class StoriesActivity: BaseThemedWithoutBarActivity() {

    companion object {
        const val STORY_CAROUSEL = "LaunchStoryCarousel"
        const val STORY_CAROUSEL_USER_ID = "LaunchStoryUserId"
    }


    private lateinit var binding: ActivityStoriesBinding

    private lateinit var model: StoriesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val carousel = intent.getSerializableExtra(STORY_CAROUSEL) as StoryCarousel
        val userId = intent.getStringExtra(STORY_CAROUSEL_USER_ID)

        binding = ActivityStoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val _model: StoriesViewModel by viewModels {
            StoriesViewModelFactory(application, carousel, userId)
        }
        model = _model

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    binding.pause.isSelected = uiState.paused

                    uiState.age?.let { setTextViewFromISO8601(it, binding.storyAge, false) }

                    if (uiState.errorMessage != null) {
                        binding.storyErrorText.setText(uiState.errorMessage)
                        binding.storyErrorCard.isVisible = true
                    } else binding.storyErrorCard.isVisible = false

                    if (uiState.snackBar != null) {
                        Snackbar.make(
                            binding.root, uiState.snackBar,
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(binding.storyReplyField).show()
                        model.shownSnackbar()
                    }

                    if (uiState.username != null) {
                        binding.storyReplyField.hint = getString(R.string.replyToStory).format(uiState.username)
                    } else binding.storyReplyField.hint = null

                    uiState.profilePicture?.let {
                        Glide.with(binding.storyAuthorProfilePicture)
                            .load(it)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.storyAuthorProfilePicture)
                    }

                    binding.storyAuthor.text = uiState.username

                    binding.carouselProgress.text = getString(R.string.storyProgress)
                            .format(uiState.currentImage + 1, uiState.imageList.size)

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
                                        Glide.with(binding.storyImage)
                                            .load(uiState.imageList.getOrNull(uiState.currentImage + 1))
                                            .preload()
                                    return false
                                }
                            })
                            .into(binding.storyImage)
                    }
                }
            }
        }

        //Pause when clicked on text field
        binding.storyReplyField.editText?.setOnFocusChangeListener { view, isFocused ->
            if (view.isInTouchMode && isFocused) {
                view.performClick()  // picks up first tap
            }
        }
        binding.storyReplyField.editText?.setOnClickListener {
            if (!model.uiState.value.paused) {
                model.pause()
            }
        }

        binding.storyReplyField.editText?.doAfterTextChanged {
            it?.let { text ->
                val string = text.toString()
                if(string != model.uiState.value.reply) model.replyChanged(string)
            }
        }

        binding.storyReplyField.setEndIconOnClickListener {
            binding.storyReplyField.editText?.text?.let { text ->
                model.sendReply(text)
            }
        }

        binding.storyErrorCard.setOnClickListener{
            model.dismissError()
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
                model.pause()
        }

        val authorOnClickListener = OnClickListener {
            if (!model.uiState.value.paused) {
                model.pause()
            }
            model.currentProfileId()?.let {
                lifecycleScope.launch {
                    Account.openAccountFromId(
                        it,
                        apiHolder.api ?: apiHolder.setToCurrentUser(),
                        this@StoriesActivity
                    )
                }
            }
        }
        binding.storyAuthorProfilePicture.setOnClickListener(authorOnClickListener)
        binding.storyAuthor.setOnClickListener(authorOnClickListener)

        val onTouchListener = OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> if (!model.uiState.value.paused) {
                    model.pause()
                }
                MotionEvent.ACTION_UP -> if(event.eventTime - event.downTime < 500) {
                    v.performClick()
                    return@OnTouchListener false
                } else model.pause()
            }

            true
        }

        binding.viewMiddle.setOnTouchListener{ v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->  model.pause()
                MotionEvent.ACTION_UP -> if(event.eventTime - event.downTime < 500) {
                    v.performClick()
                    return@setOnTouchListener false
                } else model.pause()
            }

            true
        }
        binding.viewLeft.setOnTouchListener(onTouchListener)
        binding.viewRight.setOnTouchListener(onTouchListener)

        //TODO implement hold to pause

        binding.viewRight.setOnClickListener {
            model.goToNext()
        }
        binding.viewLeft.setOnClickListener {
            model.goToPrevious()
        }
    }
}