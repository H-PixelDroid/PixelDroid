package org.pixeldroid.app.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityCollectionBinding
import org.pixeldroid.app.profile.ProfileFeedFragment.Companion.COLLECTION
import org.pixeldroid.app.profile.ProfileFeedFragment.Companion.COLLECTION_ID
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Collection
import java.lang.Exception

class CollectionActivity : BaseThemedWithBarActivity() {
    private lateinit var binding: ActivityCollectionBinding

    private lateinit var collection: Collection
    private var addCollection: Boolean = false
    private var deleteFromCollection: Boolean = false

    companion object {
        const val COLLECTION_TAG = "Collection"
        const val ADD_COLLECTION_TAG = "AddCollection"
        const val DELETE_FROM_COLLECTION_TAG = "DeleteFromCollection"
        const val DELETE_FROM_COLLECTION_RESULT = "DeleteFromCollectionResult"
        const val ADD_TO_COLLECTION_RESULT = "AddToCollectionResult"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        collection = intent.getSerializableExtra(COLLECTION_TAG) as Collection

        addCollection = intent.getBooleanExtra(ADD_COLLECTION_TAG, false)
        deleteFromCollection = intent.getBooleanExtra(DELETE_FROM_COLLECTION_TAG, false)

        val addedResult = intent.getBooleanExtra(ADD_TO_COLLECTION_RESULT, false)
        val deletedResult = intent.getBooleanExtra(DELETE_FROM_COLLECTION_RESULT, false)

        if(addedResult)
            Snackbar.make(
                binding.root, getString(R.string.added_post_to_collection),
                Snackbar.LENGTH_LONG
            ).show()
        else if (deletedResult) Snackbar.make(
            binding.root, getString(R.string.removed_post_from_collection),
            Snackbar.LENGTH_LONG
        ).show()

        supportActionBar?.title = if(addCollection) getString(R.string.add_to_collection)
                        else if(deleteFromCollection) getString(R.string.delete_from_collection)
                        else getString(R.string.collection_title).format(collection.username)

        val collectionFragment = ProfileFeedFragment()
        collectionFragment.arguments = Bundle().apply {
            putBoolean(COLLECTION, true)
            putString(COLLECTION_ID, collection.id)
            putSerializable(COLLECTION, collection)
            if(addCollection) putBoolean(ADD_COLLECTION_TAG, true)
            else if (deleteFromCollection) putBoolean(DELETE_FROM_COLLECTION_TAG, true)
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.collectionFragment, collectionFragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val userId = db.userDao().getActiveUser()?.user_id

        // Only show options for editing a collection if it's the user's collection
        if(!(addCollection || deleteFromCollection) && userId != null && collection.pid == userId) {
            val inflater: MenuInflater = menuInflater
            inflater.inflate(R.menu.collection_menu, menu)
        }
        return true
    }

    override fun onNewIntent(intent: Intent?) {
        // Relaunch same activity, to avoid duplicates in history
        super.onNewIntent(intent)
        finish()
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_collection -> {
                AlertDialog.Builder(this).apply {
                    setMessage(R.string.delete_collection_warning)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        // Delete collection
                        lifecycleScope.launch {
                            val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                            try {
                                api.deleteCollection(collection.id)
                                // Deleted, exit activity
                                finish()
                            } catch (exception: Exception) {
                                Snackbar.make(
                                    binding.root, getString(R.string.something_went_wrong),
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()
                true
            }
            R.id.add_post_collection -> {
                val intent = Intent(this, CollectionActivity::class.java)
                intent.putExtra(COLLECTION_TAG, collection)
                intent.putExtra(ADD_COLLECTION_TAG, true)
                startActivity(intent)
                true
            }
            R.id.remove_post_collection -> {
                val intent = Intent(this, CollectionActivity::class.java)
                intent.putExtra(COLLECTION_TAG, collection)
                intent.putExtra(DELETE_FROM_COLLECTION_TAG, true)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
