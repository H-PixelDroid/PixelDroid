package org.pixeldroid.app.posts

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.pixeldroid.app.databinding.ActivityAlbumBinding
import org.pixeldroid.app.utils.BaseActivity

class AlbumActivity : BaseActivity() {

    private lateinit var model: AlbumViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAlbumBinding.inflate(layoutInflater)

        val _model: AlbumViewModel by viewModels { AlbumViewModelFactory(application, intent) }
        model = _model

        setContentView(binding.root)

        binding.albumPager.adapter = AlbumViewPagerAdapter(model.uiState.value.mediaAttachments,
            sensitive = false,
            opened = true,
            //In the activity, we assume we want to show everything
            alwaysShowNsfw = true
        )
        binding.albumPager.currentItem = model.uiState.value.index

        if (model.uiState.value.mediaAttachments.size == 1) {
            binding.albumPager.isUserInputEnabled = false
        }
        else if ((model.uiState.value.mediaAttachments.size) > 1) {
            binding.postIndicator.setViewPager(binding.albumPager)
            binding.postIndicator.visibility = View.VISIBLE
        } else {
            binding.postIndicator.visibility = View.GONE
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setBackgroundDrawable(null)

        // TODO: Remove from StatusViewHolder (877-893)
        // TODO: Issue is that albumPager does not listen to the clicks here
        binding.albumPager.setOnClickListener {
            val windowInsetsController =
                WindowCompat.getInsetsController(this.window, it)
            // Configure the behavior of the hidden system bars
            if (model.uiState.value.isActionBarHidden) {
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both the status bar and the navigation bar
                supportActionBar?.show()
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                model.uiState.value.isActionBarHidden = false
            } else {
                // Configure the behavior of the hidden system bars
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both the status bar and the navigation bar
                supportActionBar?.hide()
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                model.uiState.value.isActionBarHidden = true
            }
        }

    }
}