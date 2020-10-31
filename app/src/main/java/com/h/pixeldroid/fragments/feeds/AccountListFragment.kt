package com.h.pixeldroid.fragments.feeds

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Account.Companion.ACCOUNT_ID_TAG
import com.h.pixeldroid.objects.Account.Companion.FOLLOWERS_TAG
import kotlinx.android.synthetic.main.account_list_entry.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class AccountListFragment : FeedFragment() {
    lateinit var profilePicRequest: RequestBuilder<Drawable>
    protected lateinit var adapter : FeedsRecyclerViewAdapter<Account, AccountsRecyclerViewAdapter.ViewHolder>
    lateinit var factory: FeedDataSourceFactory<String, Account>
    lateinit var content: LiveData<PagedList<Account>>

    private var currentPage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        //RequestBuilder that is re-used for every image
        profilePicRequest = Glide.with(this)
            .asDrawable().dontAnimate().apply(RequestOptions().circleCrop())
            .placeholder(R.drawable.ic_default_user)

        adapter = AccountsRecyclerViewAdapter()
        list.adapter = adapter

        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Account> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Account> = RecyclerViewPreloader(
            Glide.with(this), adapter as AccountListFragment.AccountsRecyclerViewAdapter, sizeProvider, 4
        )
        list.addOnScrollListener(preloader)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content = makeContent()

        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })

        swipeRefreshLayout.setOnRefreshListener {
            showError(show = false)

            currentPage = 0
            //by invalidating data, loadInitial will be called again
            factory.liveData.value!!.invalidate()
        }
    }

    internal open fun makeContent(): LiveData<PagedList<Account>> {
        val id = arguments?.getSerializable(ACCOUNT_ID_TAG) as String
        val following = arguments?.getSerializable(FOLLOWERS_TAG) as Boolean

        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = AccountListDataSource(following, id)
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder(factory, config).build()
    }

    inner class AccountListDataSource(private val following: Boolean, private val id: String) :
        FeedDataSource<String, Account>() {

        override fun newSource(): AccountListDataSource {
            return AccountListDataSource(following, id)
        }

        //We use the id as the key
        override fun getKey(item: Account): String {
            return currentPage.toString()
        }

        override fun makeInitialCall(requestedLoadSize: Int): Call<List<Account>> {
            return if (following) {
                pixelfedAPI.followers(
                    id, "Bearer $accessToken",
                    limit = requestedLoadSize
                )
            } else {
                pixelfedAPI.following(
                    id, "Bearer $accessToken",
                    limit = requestedLoadSize
                )
            }
        }

        override fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Account>> {
            // Pixelfed and Mastodon don't implement this in the same fashion. Pixelfed uses
            // Laravel's paging mechanism, while Mastodon uses the Link header for pagination.
            // No need to know which is which, they should ignore the non-relevant argument
            return if (following) {
                pixelfedAPI.followers(
                    id, "Bearer $accessToken",
                    limit = requestedLoadSize, page = key, max_id = key
                )
            } else {
                pixelfedAPI.following(
                    id, "Bearer $accessToken",
                    limit = requestedLoadSize, page = key, max_id = key
                )
            }
        }

        override fun enqueueCall(call: Call<List<Account>>, callback: LoadCallback<Account>){

            call.enqueue(object : Callback<List<Account>> {
                override fun onResponse(call: Call<List<Account>>, response: Response<List<Account>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        if(response.headers()["Link"] != null){
                            //Header is of the form:
                            // Link: <https://mastodon.social/api/v1/accounts/1/followers?limit=2&max_id=7628164>; rel="next", <https://mastodon.social/api/v1/accounts/1/followers?limit=2&since_id=7628165>; rel="prev"
                            // So we want the first max_id value. In case there are arguments after
                            // the max_id in the URL, we make sure to stop at the first '?'
                            currentPage = response.headers()["Link"]
                                .orEmpty()
                                .substringAfter("max_id=")
                                .substringBefore('?')
                                .substringBefore('>')
                                .toIntOrNull() ?: 0
                        } else {
                            currentPage++
                        }
                        callback.onResult(data)
                    } else{
                        showError()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<Account>>, t: Throwable) {
                    showError(errorText = R.string.feed_failed)
                    Log.e("AccountListFragment", t.toString())
                }
            })
        }
    }

    inner class AccountsRecyclerViewAdapter : FeedsRecyclerViewAdapter<Account, AccountsRecyclerViewAdapter.ViewHolder>(),
        ListPreloader.PreloadModelProvider<Account> {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.account_list_entry, parent, false)
            context = view.context
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder : ViewHolder, position : Int) {
            val account = getItem(position) ?: return
            profilePicRequest.load(account.avatar).into(holder.avatar)

            holder.username.text = account.username
            @SuppressLint("SetTextI18n")
            holder.acct.text = "@${account.acct}"

            holder.mView.setOnClickListener { account.openProfile(context) }
        }

        inner class ViewHolder(val mView : View) : RecyclerView.ViewHolder(mView) {
            val avatar : ImageView = mView.account_entry_avatar
            val username : TextView = mView.account_entry_username
            val acct: TextView = mView.account_entry_acct
        }

        override fun getPreloadItems(position : Int) : MutableList<Account> {
            val account = getItem(position) ?: return mutableListOf()
            return mutableListOf(account)
        }

        override fun getPreloadRequestBuilder(item : Account) : RequestBuilder<*>? {
            return profilePicRequest.load(item.avatar_static)
        }
    }
}