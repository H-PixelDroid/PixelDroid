package org.pixeldroid.app.postCreation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
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

        fun intentForUris(context: Context, uris: List<Uri>) =
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                // Pass downloaded images to new post creation activity
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM, ArrayList(uris)
                )

                uris.forEach {
                    // Why are we using ClipData in addition to parcelableArrayListExtra here?
                    // Because the FLAG_GRANT_READ_URI_PERMISSION needs to be applied to the URIs, and
                    // for some reason it doesn't get applied to all of them when not using ClipData
                    if (clipData == null) {
                        clipData = ClipData("", emptyArray(), ClipData.Item(it))
                    } else {
                        clipData!!.addItem(ClipData.Item(it))
                    }
                }

                setClass(context, PostCreationActivity::class.java)

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }

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