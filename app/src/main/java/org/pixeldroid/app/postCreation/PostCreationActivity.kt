package org.pixeldroid.app.postCreation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.utils.BaseActivity

class PostCreationActivity : BaseActivity() {

    companion object {
        internal const val POST_DESCRIPTION = "post_description"
        internal const val PICTURE_DESCRIPTIONS = "picture_descriptions"
        internal const val POST_REDRAFT = "post_redraft"
        internal const val POST_NSFW = "post_nsfw"
        internal const val TEMP_FILES = "temp_files"
    }

    private lateinit var binding: ActivityPostCreationBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.postCreationContainer) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.post_creation_graph)
    }

    override fun onSupportNavigateUp() = navController.navigateUp() || super.onSupportNavigateUp()
}