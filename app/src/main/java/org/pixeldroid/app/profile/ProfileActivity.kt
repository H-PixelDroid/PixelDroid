package org.pixeldroid.app.profile

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
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityProfileBinding
import org.pixeldroid.app.databinding.FragmentProfilePostsBinding
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.posts.feeds.initAdapter
import org.pixeldroid.app.posts.feeds.launch
import org.pixeldroid.app.posts.feeds.uncachedFeeds.FeedViewModel
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedContentRepository
import org.pixeldroid.app.posts.feeds.uncachedFeeds.profile.ProfileContentRepository
import org.pixeldroid.app.posts.parseHTMLText
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.ImageConverter
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.openUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.pixeldroid.app.utils.api.objects.Attachment
import retrofit2.HttpException
import java.io.IOException

class ProfileActivity : BaseActivity() {

    private lateinit var domain : String
    private lateinit var accountId : String
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileAdapter: PagingDataAdapter<Status, RecyclerView.ViewHolder>
    private lateinit var viewModel: FeedViewModel<Status>

    private var user: UserDatabaseEntity? = null
    private var job: Job? = null

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        user = db.userDao().getActiveUser()

        domain = user?.instance_uri.orEmpty()

        // Set profile according to given account
        val account = intent.getSerializableExtra(Account.ACCOUNT_TAG) as Account?
        accountId = account?.id ?: user!!.user_id

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ProfileViewModelFactory(
                ProfileContentRepository(
                    apiHolder.setToCurrentUser(),
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
        job = launch(job, lifecycleScope, viewModel, profileAdapter)
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

    private fun setContent(account: Account?) {
        if(account != null) {
            setViews(account)
        } else {
            lifecycleScope.launchWhenResumed {
                val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                val myAccount: Account = try {
                    api.verifyCredentials()
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
            account.anyAvatar(),
            profilePicture
        )

        binding.descriptionTextView.text = parseHTMLText(
                account.note ?: "", emptyList(), apiHolder,
                applicationContext,
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
                val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                val relationship = api.checkRelationships(
                    listOf(account.id.orEmpty())
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
                        val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                        val rel = api.follow(account.id.orEmpty())
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
                        val api: PixelfedAPI = apiHolder.api ?: apiHolder.setToCurrentUser()
                        val rel = api.unfollow(account.id.orEmpty())
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
    private val videoIcon: ImageView = binding.videoIcon

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
            if(post.media_attachments?.get(0)?.type == Attachment.AttachmentType.video) {
                videoIcon.visibility = View.VISIBLE
            } else videoIcon.visibility = View.GONE

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
