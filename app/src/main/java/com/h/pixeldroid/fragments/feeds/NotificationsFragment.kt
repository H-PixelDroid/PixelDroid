package com.h.pixeldroid.fragments.feeds

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.h.pixeldroid.PostActivity
import com.h.pixeldroid.ProfileActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Notification
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.HtmlUtils.Companion.parseHTMLText
import com.h.pixeldroid.utils.Utils.Companion.setTextViewFromISO8601
import kotlinx.android.synthetic.main.fragment_notifications.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * A fragment representing a list of Items.
 */
class NotificationsFragment : FeedFragment() {

    lateinit var profilePicRequest: RequestBuilder<Drawable>
    protected lateinit var adapter : FeedsRecyclerViewAdapter<Notification, NotificationsRecyclerViewAdapter.ViewHolder>
    lateinit var factory: FeedDataSourceFactory<String, Notification>



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        //RequestBuilder that is re-used for every image
        profilePicRequest = Glide.with(this)
            .asDrawable().apply(RequestOptions().circleCrop())
            .placeholder(R.drawable.ic_default_user)


        adapter = NotificationsRecyclerViewAdapter()
        list.adapter = adapter


        //Make Glide be aware of the recyclerview and pre-load images
        val sizeProvider: ListPreloader.PreloadSizeProvider<Notification> = ViewPreloadSizeProvider()
        val preloader: RecyclerViewPreloader<Notification> = RecyclerViewPreloader(
            Glide.with(this), adapter as NotificationsFragment.NotificationsRecyclerViewAdapter, sizeProvider, 4
        )
        list.addOnScrollListener(preloader)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = makeContent()

        content.observe(viewLifecycleOwner,
            Observer { c ->
                adapter.submitList(c)
                //after a refresh is done we need to stop the pull to refresh spinner
                swipeRefreshLayout.isRefreshing = false
            })

        swipeRefreshLayout.setOnRefreshListener {
            showError(show = false)

            //by invalidating data, loadInitial will be called again
            factory.liveData.value!!.invalidate()
        }
    }

    private fun makeContent(): LiveData<PagedList<Notification>> {
        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = NotificationListDataSource()
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder(factory, config).build()
    }

    inner class NotificationListDataSource: FeedDataSource<String, Notification>() {

        override fun newSource(): NotificationListDataSource {
            return NotificationListDataSource()
        }

        //We use the id as the key
        override fun getKey(item: Notification): String {
            return item.id
        }

        override fun makeInitialCall(requestedLoadSize: Int): Call<List<Notification>> {
            return pixelfedAPI
                .notifications("Bearer $accessToken", limit="$requestedLoadSize")
        }
        override fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Notification>> {
            return pixelfedAPI
                .notifications("Bearer $accessToken", max_id=key, limit="$requestedLoadSize")
        }

        override fun enqueueCall(call: Call<List<Notification>>, callback: LoadCallback<Notification>){

            call.enqueue(object : Callback<List<Notification>> {
                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        callback.onResult(data)
                    } else {
                        showError()
                    }
                    swipeRefreshLayout.isRefreshing = false
                    loadingIndicator.visibility = View.GONE
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    showError(errorText = R.string.feed_failed)
                    Log.e("NotificationsFragment", t.toString())
                }
            })
        }
    }

    /**
     * [RecyclerView.Adapter] that can display a [Notification]
     */
    inner class NotificationsRecyclerViewAdapter: FeedsRecyclerViewAdapter<Notification, NotificationsRecyclerViewAdapter.ViewHolder>(),
        ListPreloader.PreloadModelProvider<Notification> {

        private val mOnClickListener: View.OnClickListener

        init {
            mOnClickListener = View.OnClickListener { v ->
                val notification = v.tag as Notification
                openActivity(notification)
            }
        }

        private fun openPostFromNotifcation(notification: Notification) : Intent {
            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra(Status.POST_TAG, notification.status)
            return  intent
        }

        private fun openActivity(notification: Notification){
            val intent: Intent
            when (notification.type){
                Notification.NotificationType.mention, Notification.NotificationType.favourite-> {
                    intent = openPostFromNotifcation(notification)
                }
                Notification.NotificationType.reblog-> {
                    intent = openPostFromNotifcation(notification)
                }
                Notification.NotificationType.follow -> {
                    intent = Intent(context, ProfileActivity::class.java)
                    intent.putExtra(Account.ACCOUNT_TAG, notification.account)
                }
            }
            context.startActivity(intent)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_notifications, parent, false)
            context = view.context
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = getItem(position) ?: return
            profilePicRequest.load(notification.account.avatar_static).into(holder.avatar)

            val previewUrl = notification.status?.media_attachments?.getOrNull(0)?.preview_url
            if(!previewUrl.isNullOrBlank()){
                Glide.with(holder.mView).load(previewUrl)
                    .placeholder(R.drawable.ic_picture_fallback).into(holder.photoThumbnail)
            } else{
                holder.photoThumbnail.visibility = View.GONE
            }

            setNotificationType(notification.type, notification.account.username, holder.notificationType)
            setTextViewFromISO8601(notification.created_at, holder.notificationTime, false, context)

            //Convert HTML to clickable text
            holder.postDescription.text =
                parseHTMLText(
                    notification.status?.content ?: "",
                    notification.status?.mentions,
                    pixelfedAPI,
                    context,
                    "Bearer $accessToken"
                )


            with(holder.mView) {
                tag = notification
                setOnClickListener(mOnClickListener)
            }
        }

        private fun setNotificationType(type: Notification.NotificationType, username: String,
                                        textView: TextView
        ){
            val context = textView.context
            val (format: String, drawable: Drawable?) = when(type) {
                Notification.NotificationType.follow -> {
                    setNotificationTypeTextView(context, R.string.followed_notification, R.drawable.ic_follow)
                }
                Notification.NotificationType.mention -> {
                    setNotificationTypeTextView(context, R.string.mention_notification, R.drawable.ic_apenstaart)
                }

                Notification.NotificationType.reblog -> {
                    setNotificationTypeTextView(context, R.string.shared_notification, R.drawable.ic_reblog_blue)
                }

                Notification.NotificationType.favourite -> {
                    setNotificationTypeTextView(context, R.string.liked_notification, R.drawable.ic_like_full)
                }
            }
            textView.text = format.format(username)
            textView.setCompoundDrawablesWithIntrinsicBounds(
                drawable,null,null,null
            )
        }
        private fun setNotificationTypeTextView(context: Context, format: Int, drawable: Int): Pair<String, Drawable?> {
            return Pair(context.getString(format), context.getDrawable(drawable))
        }


        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            val notificationType: TextView = mView.notification_type
            val notificationTime: TextView = mView.notification_time
            val postDescription: TextView = mView.notification_post_description
            val avatar: ImageView = mView.notification_avatar
            val photoThumbnail: ImageView = mView.notification_photo_thumbnail
        }

        override fun getPreloadItems(position: Int): MutableList<Notification> {
            val notification = getItem(position) ?: return mutableListOf()
            return mutableListOf(notification)
        }

        override fun getPreloadRequestBuilder(item: Notification): RequestBuilder<*>? {
            return profilePicRequest.load(item.account.avatar_static)
        }
    }
}