package org.pixeldroid.app.posts

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.ActivityAlbumBinding


class AlbumActivity : AppCompatActivity() {
    private val model: AlbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.albumPager.adapter = AlbumViewPagerAdapter(
            model.uiState.value.mediaAttachments,
            sensitive = false,
            opened = true,
            //In the activity, we assume we want to show everything
            alwaysShowNsfw = true,
            clickCallback = ::clickCallback
        )

        binding.albumPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { model.positionSelected(position) }
        })

        if (model.uiState.value.mediaAttachments.size == 1) {
            binding.albumPager.isUserInputEnabled = false
        } else if ((model.uiState.value.mediaAttachments.size) > 1) {
            binding.postIndicator.setViewPager(binding.albumPager)
            binding.postIndicator.visibility = View.VISIBLE
        } else {
            binding.postIndicator.visibility = View.GONE
        }

        // Not really necessary because the ViewPager saves its state in onSaveInstanceState, but
        // it's good to stay consistent in case something gets out of sync
        binding.albumPager.setCurrentItem(model.uiState.value.index, false)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setBackgroundDrawable(null)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { uiState ->
                    binding.albumPager.currentItem = uiState.index
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.isActionBarHidden.collect { isActionBarHidden ->
                    val windowInsetsController =
                        WindowCompat.getInsetsController(this@AlbumActivity.window, binding.albumPager)
                    if (isActionBarHidden) {
                        // Configure the behavior of the hidden system bars
                        windowInsetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        // Hide both the status bar and the navigation bar
                        supportActionBar?.hide()
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        binding.postIndicator.visibility = View.GONE
                    } else {
                        // Configure the behavior of the hidden system bars
                        windowInsetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        // Show both the status bar and the navigation bar
                        supportActionBar?.show()
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        if ((model.uiState.value.mediaAttachments.size) > 1) {
                            binding.postIndicator.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    /**
     * Callback passed to the AlbumViewPagerAdapter to signal a single click on the image
     */
    private fun clickCallback(){
        model.barHide()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle up arrow manually,
                // since "up" isn't defined for this activity
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}