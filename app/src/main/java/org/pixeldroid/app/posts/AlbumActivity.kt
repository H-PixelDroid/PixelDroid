package org.pixeldroid.app.posts

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.pixeldroid.app.databinding.ActivityAlbumBinding
import org.pixeldroid.app.utils.api.objects.Attachment

class AlbumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAlbumBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val mediaAttachments = intent.getSerializableExtra("images") as ArrayList<Attachment>
        val index = intent.getIntExtra("index", 0)
        binding.albumPager.adapter = AlbumViewPagerAdapter(mediaAttachments,
            sensitive = false,
            opened = true,
            //In the activity, we assume we want to show everything
            alwaysShowNsfw = true
        )
        binding.albumPager.currentItem = index

        if(mediaAttachments.size == 1){
            binding.albumPager.isUserInputEnabled = false
        }
        else if((mediaAttachments.size) > 1) {
            binding.postIndicator.setViewPager(binding.albumPager)
            binding.postIndicator.visibility = View.VISIBLE
        } else {
            binding.postIndicator.visibility = View.GONE
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setBackgroundDrawable(null)
    }
}