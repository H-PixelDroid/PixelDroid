package org.pixeldroid.app.postCreation

import android.os.*
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPostCreationBinding
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

const val TAG = "Post Creation Activity"

class PostCreationActivity : BaseActivity() {

    companion object {
        internal const val PICTURE_DESCRIPTION = "picture_description"
        internal const val POST_REDRAFT = "post_redraft"
        internal const val POST_NSFW = "post_nsfw"
        internal const val TEMP_FILES = "temp_files"
    }

    private var user: UserDatabaseEntity? = null
    private lateinit var instance: InstanceDatabaseEntity

    private lateinit var binding: ActivityPostCreationBinding

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = db.userDao().getActiveUser()

        instance = user?.run {
            db.instanceDao().getAll().first { instanceDatabaseEntity ->
                instanceDatabaseEntity.uri.contains(instance_uri)
            }
        } ?: InstanceDatabaseEntity("", "")

        binding = ActivityPostCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.postCreationContainer) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.post_creation_graph)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}