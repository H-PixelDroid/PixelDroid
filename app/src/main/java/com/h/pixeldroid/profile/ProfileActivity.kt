package com.h.pixeldroid.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.databinding.ActivityProfileBinding
import com.h.pixeldroid.databinding.FragmentProfilePostsBinding
import com.h.pixeldroid.posts.PostActivity
import com.h.pixeldroid.posts.feeds.ReposLoadStateAdapter
import com.h.pixeldroid.posts.feeds.uncachedFeeds.FeedViewModel
import com.h.pixeldroid.posts.feeds.uncachedFeeds.UncachedContentRepository
import com.h.pixeldroid.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import com.h.pixeldroid.posts.parseHTMLText
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.openUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
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
        initAdapter(binding, profileAdapter)

        binding.profilePostsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        binding.profileRefreshLayout.setOnRefreshListener {
            //It shouldn't be necessary to also retry() in addition to refresh(),
            //but if we don't do this, reloads after an error fail immediately...
            profileAdapter.retry()
            profileAdapter.refresh()
        }

        setContent(account)
        profileLaunch()
        profileInitSearch()
    }

    private fun profileLaunch() {
        // Make sure we cancel the previous job before creating a new one
        job?.cancel()
        job = lifecycleScope.launch {
            viewModel.flow().collectLatest {
                profileAdapter.submitData(it)
            }
        }
    }

    private fun profileInitSearch() {
        // Scroll to top when the list is refreshed from network.
        lifecycleScope.launch {
            profileAdapter.loadStateFlow
                    // Only emit when REFRESH LoadState for RemoteMediator changes.
                    .distinctUntilChangedBy { it.refresh }
                    // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                    .filter { it.refresh is LoadState.NotLoading }
                    .collect { binding.profilePostsRecyclerView.scrollToPosition(0) }
        }
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

    /**
     * Initialises the [RecyclerView] adapter for the different FeedFragments.
     *
     * Makes the UI respond to various [LoadState]s, including errors when an error message is shown.
     */
    internal fun <T: Any> initAdapter(binding: ActivityProfileBinding, adapter: PagingDataAdapter<T, RecyclerView.ViewHolder>) {
        binding.profilePostsRecyclerView.adapter = adapter.withLoadStateFooter(
                footer = ReposLoadStateAdapter { adapter.retry() }
        )

        adapter.addLoadStateListener { loadState ->

            if(!binding.profileProgressBar.isVisible && binding.profileRefreshLayout.isRefreshing) {
                // Stop loading spinner when loading is done
                binding.profileRefreshLayout.isRefreshing = loadState.refresh is LoadState.Loading
            } else {
                // ProgressBar should stop showing as soon as the source stops loading ("source"
                // meaning the database, so don't wait on the network)
                val sourceLoading = loadState.source.refresh is LoadState.Loading
                if(!sourceLoading && binding.profilePostsRecyclerView.size > 0){
                    binding.profilePostsRecyclerView.isVisible = true
                    binding.profileProgressBar.isVisible = false
                } else if(binding.profilePostsRecyclerView.size ==  0
                        && loadState.append is LoadState.NotLoading
                        && loadState.append.endOfPaginationReached){
                    binding.profileProgressBar.isVisible = false
                    showError(errorText = "Nothing to see here :(")
                }
            }


            // Toast on any error, regardless of whether it came from RemoteMediator or PagingSource
            val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.source.refresh as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                    ?: loadState.refresh as? LoadState.Error
            errorState?.let {
                showError(errorText = it.error.toString())
            }
            if (errorState == null) showError(show = false, errorText = "")
        }

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
            Log.e("ProfileActivity", "Cannot open this link")
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
                    if (relationship.following) {
                        setOnClickUnfollow(account)
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
                        pixelfedAPI.follow(account.id.orEmpty(), "Bearer $accessToken")
                        setOnClickUnfollow(account)
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

    private fun setOnClickUnfollow(account: Account) {
        binding.followButton.apply {
            setText(R.string.unfollow)

            setOnClickListener {
                lifecycleScope.launchWhenResumed {
                    try {
                        pixelfedAPI.unfollow(account.id.orEmpty(), "Bearer $accessToken")
                        setOnClickFollow(account)
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
