package com.h.pixeldroid.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityProfileBinding
import com.h.pixeldroid.databinding.FragmentProfilePostsBinding
import com.h.pixeldroid.posts.PostActivity
import com.h.pixeldroid.posts.feeds.initAdapter
import com.h.pixeldroid.posts.feeds.launch
import com.h.pixeldroid.posts.feeds.uncachedFeeds.FeedViewModel
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedContentRepository
import com.h.pixeldroid.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import com.h.pixeldroid.posts.parseHTMLText
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.FeedContent
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.openUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProfileActivity : BaseActivity() {

    private lateinit var pixelfedAPI : PixelfedAPI
    private lateinit var accessToken : String
    private lateinit var domain : String
    private lateinit var accountId : String
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileAdapter: PagingDataAdapter<Status, RecyclerView.ViewHolder>
    private lateinit var viewModel: FeedViewModel<Status>

    private var user: UserDatabaseEntity? = null
    private var job: Job? = null

    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)
        accessToken = user?.accessToken.orEmpty()

        // Set profile according to given account
        val account = intent.getSerializableExtra(Account.ACCOUNT_TAG) as Account?
        accountId = account?.id ?: user!!.user_id

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ProfileViewModelFactory(
                ProfileContentRepository(
                        apiHolder.setDomainToCurrentUser(db),
                        db.userDao().getActiveUser()!!.accessToken,
                        accountId
                )
            )
        ).get(FeedViewModel::class.java) as FeedViewModel<Status>

        profileAdapter = ProfilePostsAdapter()
        initAdapter(binding.profileProgressBar, binding.profileRefreshLayout,
            binding.profilePostsRecyclerView, binding.motionLayout, binding.profileErrorLayout,
            profileAdapter)

        binding.profilePostsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        binding.profileRefreshLayout.setOnRefreshListener {
            profileAdapter.refresh()
        }

        setContent(account)
        @Suppress("UNCHECKED_CAST")
        job = launch(job, lifecycleScope, viewModel as FeedViewModel<FeedContent>,
                profileAdapter as PagingDataAdapter<FeedContent, RecyclerView.ViewHolder>)
    }

    /**
     * Shows or hides the error in the different FeedFragments
     */
    private fun showError(errorText: String = "Something went wrong while loading", show: Boolean = true){
        if(show){
            binding.profileProgressBar.visibility = View.GONE
            binding.motionLayout.transitionToEnd()
        } else if(binding.motionLayout.progress == 1F) {
            binding.motionLayout.transitionToStart()
        }
        binding.profileRefreshLayout.isRefreshing = false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setContent(account: Account?) {
        if(account != null) {
            setViews(account)
        } else {
            lifecycleScope.launchWhenResumed {
                val myAccount: Account = try {
                    pixelfedAPI.verifyCredentials("Bearer $accessToken")
                } catch (exception: IOException) {
                    Log.e("ProfileActivity:", exception.toString())
                    return@launchWhenResumed showError()
                } catch (exception: HttpException) {
                    return@launchWhenResumed showError()
                }
                setViews(myAccount)
            }
        }

        if(account != null && account.id != user?.user_id) {
            //if we aren't viewing our own account, activate follow button
            activateFollow(account)
        } else {
            //if we *are* viewing our own account, activate the edit button
            activateEditButton()
        }

        // On click open followers list
        binding.nbFollowersTextView.setOnClickListener{ onClickFollowers(account) }
        // On click open followers list
        binding.nbFollowingTextView.setOnClickListener{ onClickFollowing(account) }
    }

    /**
     * Populate profile page with user's data
     */
    private fun setViews(account: Account) {
        val profilePicture = binding.profilePictureImageView
        ImageConverter.setRoundImageFromURL(
            View(applicationContext),
            account.avatar,
            profilePicture
        )

        binding.descriptionTextView.text = parseHTMLText(
            account.note ?: "", emptyList(), pixelfedAPI,
            applicationContext, "Bearer $accessToken",
            lifecycleScope
        )

        val displayName = account.getDisplayName()

        binding.accountNameTextView.text = displayName

        supportActionBar?.title = displayName
        if(displayName != "@${account.acct}") {
            supportActionBar?.subtitle = "@${account.acct}"
        }

        binding.nbPostsTextView.text = resources.getQuantityString(
                R.plurals.nb_posts,
                account.statuses_count ?: 0,
                account.statuses_count ?: 0
        )

        binding.nbFollowersTextView.text = resources.getQuantityString(
                R.plurals.nb_followers,
                account.followers_count ?: 0,
                account.followers_count ?: 0
        )

        binding.nbFollowingTextView.text = resources.getQuantityString(
                R.plurals.nb_following,
                account.following_count ?: 0,
                account.following_count ?: 0
        )
    }

    private fun onClickEditButton() {
        val url = "$domain/settings/home"

        if(!openUrl(url)) {
            Snackbar.make(binding.root, getString(R.string.edit_link_failed),
                    Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onClickFollowers(account: Account?) {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWERS_TAG, true)
        intent.putExtra(Account.ACCOUNT_TAG, account)

        ContextCompat.startActivity(this, intent, null)
    }

    private fun onClickFollowing(account: Account?) {
        val intent = Intent(this, FollowsActivity::class.java)
        intent.putExtra(Account.FOLLOWERS_TAG, false)
        intent.putExtra(Account.ACCOUNT_TAG, account)

        ContextCompat.startActivity(this, intent, null)
    }

    private fun activateEditButton() {
        // Edit button redirects to Pixelfed's "edit account" page
        binding.editButton.apply {
            visibility = View.VISIBLE
            setOnClickListener{ onClickEditButton() }
        }
    }

    /**
     * Set up follow button
     */
    private fun activateFollow(account: Account) {
        // Get relationship between the two users (credential and this) and set followButton accordingly
        lifecycleScope.launch {
            try {
                val relationship = pixelfedAPI.checkRelationships(
                    "Bearer $accessToken", listOf(account.id.orEmpty())
                ).firstOrNull()

                if(relationship != null){
                    if (relationship.following == true || relationship.requested == true) {
                        setOnClickUnfollow(account, relationship.requested == true)
                    } else {
                        setOnClickFollow(account)
                    }
                    binding.followButton.visibility = View.VISIBLE
                }
            } catch (exception: IOException) {
                Log.e("FOLLOW ERROR", exception.toString())
                Toast.makeText(
                    applicationContext, getString(R.string.follow_status_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (exception: HttpException) {
                Toast.makeText(
                    applicationContext, getString(R.string.follow_button_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setOnClickFollow(account: Account) {
        binding.followButton.apply {
            setText(R.string.follow)
            setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    try {
                        val rel = pixelfedAPI.follow(account.id.orEmpty(), "Bearer $accessToken")
                        if(rel.following == true) setOnClickUnfollow(account, rel.requested == true)
                        else setOnClickFollow(account)
                    } catch (exception: IOException) {
                        Log.e("FOLLOW ERROR", exception.toString())
                        Toast.makeText(
                            applicationContext, getString(R.string.follow_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: HttpException) {
                        Toast.makeText(
                            applicationContext, getString(R.string.follow_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setOnClickUnfollow(account: Account, requested: Boolean) {
        binding.followButton.apply {
            if(account.locked == true && requested) {
                setText(R.string.follow_requested)
            } else setText(R.string.unfollow)


            fun unfollow() {
                lifecycleScope.launchWhenResumed {
                    try {
                        val rel = pixelfedAPI.unfollow(account.id.orEmpty(), "Bearer $accessToken")
                        if(rel.following == false && rel.requested == false) setOnClickFollow(account)
                        else setOnClickUnfollow(account, rel.requested == true)
                    } catch (exception: IOException) {
                        Log.e("FOLLOW ERROR", exception.toString())
                        Toast.makeText(
                                applicationContext, getString(R.string.unfollow_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: HttpException) {
                        Toast.makeText(
                                applicationContext, getString(R.string.unfollow_error),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            setOnClickListener {
                if(account.locked == true && requested){
                    AlertDialog.Builder(context)
                            .setMessage(R.string.dialog_message_cancel_follow_request)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                unfollow()
                            }
                            .setNegativeButton(android.R.string.cancel){_, _ -> }
                            .show()
                } else unfollow()
            }
        }
    }
}


class ProfileViewModelFactory @ExperimentalPagingApi constructor(
        private val searchContentRepository: UncachedContentRepository<Status>
) : ViewModelProvider.Factory {

    @ExperimentalPagingApi
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(searchContentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class ProfilePostsViewHolder(binding: FragmentProfilePostsBinding) : RecyclerView.ViewHolder(binding.root) {
    private val postPreview: ImageView = binding.postPreview
    private val albumIcon: ImageView = binding.albumIcon

    fun bind(post: Status) {

        if(post.sensitive!!) {
            ImageConverter.setSquareImageFromDrawable(
                    itemView,
                    AppCompatResources.getDrawable(itemView.context, R.drawable.ic_sensitive),
                    postPreview
            )
        } else {
            ImageConverter.setSquareImageFromURL(itemView, post.getPostPreviewURL(), postPreview)
        }

        if(post.media_attachments?.size ?: 0 > 1) {
            albumIcon.visibility = View.VISIBLE
        } else {
            albumIcon.visibility = View.GONE
        }

        postPreview.setOnClickListener {
            val intent = Intent(postPreview.context, PostActivity::class.java)
            intent.putExtra(Status.POST_TAG, post)
            postPreview.context.startActivity(intent)
        }
    }

    companion object {
        fun create(parent: ViewGroup): ProfilePostsViewHolder {
            val itemBinding = FragmentProfilePostsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
            )
            return ProfilePostsViewHolder(itemBinding)
        }
    }
}


class ProfilePostsAdapter : PagingDataAdapter<Status, RecyclerView.ViewHolder>(
        UIMODEL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProfilePostsViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = getItem(position)

        post?.let {
            (holder as ProfilePostsViewHolder).bind(it)
        }
    }

    companion object {
        private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Status>() {
            override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean =
                    oldItem.content == newItem.content
        }
    }

}
