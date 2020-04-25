package com.h.pixeldroid.fragments.feeds

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.android.synthetic.main.fragment_notifications.view.*
import retrofit2.Call


/**
 * A fragment representing a list of Items.
 */
class NotificationsFragment : FeedFragment<Notification, NotificationsFragment.NotificationsRecyclerViewAdapter.ViewHolder>() {

    lateinit var profilePicRequest: RequestBuilder<Drawable>

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
            Glide.with(this), adapter, sizeProvider, 4
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
    }

    private fun makeContent(): LiveData<PagedList<Notification>> {
        fun makeInitialCall(requestedLoadSize: Int): Call<List<Notification>> {
            return pixelfedAPI
                .notifications("Bearer $accessToken", min_id="1", limit="$requestedLoadSize")
        }
        fun makeAfterCall(requestedLoadSize: Int, key: String): Call<List<Notification>> {
            return pixelfedAPI
                .notifications("Bearer $accessToken", max_id=key, limit="$requestedLoadSize")
        }

        val config: PagedList.Config = PagedList.Config.Builder().setPageSize(10).build()
        val dataSource = FeedDataSource(::makeInitialCall, ::makeAfterCall)
        factory = FeedDataSourceFactory(dataSource)
        return LivePagedListBuilder<String, Notification>(factory, config).build()
    }

    /**
     * [RecyclerView.Adapter] that can display a [Notification]
     */
    inner class NotificationsRecyclerViewAdapter: FeedsRecyclerViewAdapter<Notification, NotificationsRecyclerViewAdapter.ViewHolder>() {

        private val mOnClickListener: View.OnClickListener

        init {
            mOnClickListener = View.OnClickListener { v ->
                val notification = v.tag as Notification
                openActivity(notification)
            }
        }
        private fun openActivity(notification: Notification){
            val intent: Intent
            when (notification.type){
                Notification.NotificationType.mention, Notification.NotificationType.favourite-> {
                    intent = Intent(context, PostActivity::class.java)
                    intent.putExtra(Status.POST_TAG, notification.status)
                }
                Notification.NotificationType.reblog-> {
                    Toast.makeText(context,"Can't see shares yet, sorry!", Toast.LENGTH_SHORT).show()
                    return
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