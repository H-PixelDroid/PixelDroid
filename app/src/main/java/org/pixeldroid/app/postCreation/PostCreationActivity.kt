package org.pixeldroid.app.postCreation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.settings.TutorialSettingsDialog.Companion.START_TUTORIAL
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

        lifecycleScope.launch {
            var targetCamera = findViewById<View>(R.id.toggleStoryPost)
            while (targetCamera == null) {
                targetCamera = findViewById(R.id.toggleStoryPost)
                delay(100)
            }
            TapTargetView.showFor(
                this@PostCreationActivity,  // `this` is an Activity
                TapTarget.forView(targetCamera, "Story or Post?",
                    "Stories are short-lived: engage your followers and keep them coming back for more. Try them out!")
                    .transparentTarget(true)
                    .targetRadius(60),
                object : TapTargetView.Listener() {
                    // The listener can listen for regular clicks, long clicks or cancels
                    override fun onTargetClick(view: TapTargetView?) {
                        super.onTargetClick(view) // This call is optional
                        findViewById<View>(R.id.buttonStory)?.performClick()
                        TapTargetView.showFor(
                            this@PostCreationActivity,  // `this` is an Activity
                            TapTarget.forView(findViewById(R.id.editPhotoButton), "Edit your picture to make it shine ✨",
                                "You can add filters, draw or add text, edit video, and more! \uD83D\uDCF7")
                                .transparentTarget(true)
                                .targetRadius(60),
                            object : TapTargetView.Listener() {
                                // The listener can listen for regular clicks, long clicks or cancels
                                override fun onTargetClick(view: TapTargetView?) {
                                    super.onTargetClick(view) // This call is optional
                                    findViewById<View>(R.id.editPhotoButton)?.performClick()
                                    TapTargetView.showFor(
                                        this@PostCreationActivity,  // `this` is an Activity
                                        TapTarget.forView(findViewById(R.id.tv_caption), "Don't forget to add a media description!",
                                            "This helps make Pixelfed accessible to everyone, and also lets you clarify what we're supposed to see in your pretty image ;)")
                                            .transparentTarget(true)
                                            .targetRadius(60),
                                        object : TapTargetView.Listener() {
                                            // The listener can listen for regular clicks, long clicks or cancels
                                            override fun onTargetClick(view: TapTargetView?) {
                                                super.onTargetClick(view) // This call is optional
                                                findViewById<View>(R.id.tv_caption)?.performClick()
                                                lifecycleScope.launch {
                                                    delay(1000)
                                                    var tv_caption = findViewById<View>(R.id.tv_caption)
                                                    while (tv_caption == null || tv_caption.visibility != View.VISIBLE) {
                                                        tv_caption = findViewById(R.id.tv_caption)
                                                        delay(100)
                                                    }
                                                    TapTargetView.showFor(
                                                        this@PostCreationActivity,  // `this` is an Activity
                                                        TapTarget.forView(findViewById(R.id.post_creation_next_button), "Take a picture to share",
                                                            "It doesn't have to be very good for now")
                                                            .transparentTarget(true)
                                                            .targetRadius(60),
                                                        object : TapTargetView.Listener() {
                                                            // The listener can listen for regular clicks, long clicks or cancels
                                                            override fun onTargetClick(view: TapTargetView?) {
                                                                super.onTargetClick(view) // This call is optional
                                                                findViewById<View>(R.id.post_creation_next_button)?.performClick()
                                                                showAccountChooser()
                                                            }
                                                        })
                                                }
                                            }
                                        })
                                }
                            })
                    }
                })
        }
    }

    private fun showAccountChooser() {
        lifecycleScope.launch {
            var toolbar = findViewById<View>(R.id.top_bar) as? MaterialToolbar
            while (toolbar == null) {
                toolbar = findViewById(R.id.top_bar) as? MaterialToolbar
                delay(100)
            }

            TapTargetView.showFor(
                this@PostCreationActivity,  // `this` is an Activity
                TapTarget.forToolbarMenuItem(
                    toolbar,
                    R.id.action_switch_accounts,
                    "Switch accounts!",
                    "PixelDroid supports using multiple Pixelfed accounts. Make sure you don't post those cat pics on the dog-only instance! \uD83D\uDE31"
                )
                    .transparentTarget(true)
                    .targetRadius(60),
                object : TapTargetView.Listener() {
                    // The listener can listen for regular clicks, long clicks or cancels
                    override fun onTargetClick(view: TapTargetView?) {
                        super.onTargetClick(view) // This call is optional
                        showPostButton()
                    }
                })
        }
    }

    private fun showPostButton() {
        TapTargetView.showFor(
            this@PostCreationActivity,  // `this` is an Activity
            TapTarget.forView(findViewById(R.id.post_submission_send_button), "Final stretch! Post that picture",
                "Have fun sharing your pictures with the world! Click anywhere else to cancel and keep looking around :)")
                .transparentTarget(true)
                .targetRadius(60),
            object : TapTargetView.Listener() {
                // The listener can listen for regular clicks, long clicks or cancels
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view) // This call is optional
                    findViewById<View>(R.id.post_creation_next_button)?.performClick()
                }
            })
    }

    override fun onSupportNavigateUp() = navController.navigateUp() || super.onSupportNavigateUp()

}