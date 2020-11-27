package com.h.pixeldroid.fragments.feeds.uncachedFeeds.accountLists

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h.pixeldroid.R
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.FeedViewModel
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.UncachedFeedFragment
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.ViewModelFactory
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWERS_TAG
import kotlinx.android.synthetic.main.account_list_entry.view.*


/**
 * Fragment to show a list of [Account]s, for a list of followers or following
 */
class AccountListFragment : UncachedFeedFragment<Account>() {

    private lateinit var id: String
    private var following: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments?.getSerializable(ACCOUNT_ID_TAG) as String
        following = arguments?.getSerializable(FOLLOWERS_TAG) as Boolean

        adapter = AccountAdapter()

    }

    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = super.onCreateView(inflater, container, savedInstanceState)

        // get the view model
        @Suppress("UNCHECKED_CAST")
        viewModel = ViewModelProvider(this, ViewModelFactory(
            FollowersContentRepository(
                apiHolder.setDomainToCurrentUser(db),
                db.userDao().getActiveUser()!!.accessToken,
                id,
                following
            )
        )
        )
            .get(FeedViewModel::class.java) as FeedViewModel<Account>

        launch()
        initSearch()

        return view
    }

}


/**
 * View Holder for an [Account] RecyclerView list item.
 */
class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val avatar : ImageView = view.account_entry_avatar
    private val username : TextView = view.account_entry_username
    private val acct: TextView = view.account_entry_acct

    private var account: Account? = null

    init {
        itemView.setOnClickListener {
            account?.openProfile(itemView.context)
        }
    }

    fun bind(account: Account?) {

        this.account = account

        Glide.with(itemView)
            .load(account?.avatar_static ?: account?.avatar)
            .circleCrop().placeholder(R.drawable.ic_default_user)
            .into(avatar)

        username.text = account?.username
        @SuppressLint("SetTextI18n")
        acct.text = "@${account?.acct}"
    }

    companion object {
        fun create(parent: ViewGroup): AccountViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.account_list_entry, parent, false)
            return AccountViewHolder(view)
        }
    }
}



class AccountAdapter : PagingDataAdapter<Account, RecyclerView.ViewHolder>(
    UIMODEL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AccountViewHolder.create(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.account_list_entry
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiModel = getItem(position)
        uiModel.let {
            (holder as AccountViewHolder).bind(it)
        }
    }

    companion object {
        private val UIMODEL_COMPARATOR = object : DiffUtil.ItemCallback<Account>() {
            override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean =
                oldItem == newItem
        }
    }
}