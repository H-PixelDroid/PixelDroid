package org.pixeldroid.app.utils.notificationsWorker

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.api.PixelfedAPI.Companion.apiForUser
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.api.objects.Notification.NotificationType.*
import org.pixeldroid.app.utils.api.objects.Status
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.di.PixelfedAPIHolder
import retrofit2.HttpException
import java.io.IOException
import java.time.OffsetDateTime
import java.util.*
import javax.inject.Inject

class NotificationsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    override suspend fun doWork(): Result {

        (applicationContext as PixelDroidApplication).getAppComponent().inject(this)

        val users: List<UserDatabaseEntity> = db.userDao().getAll()

        for (user in users){
            val channelId = user.instance_uri + user.user_id

            createNotificationChannels(
                "@${user.username}@${user.instance_uri.removePrefix("https://")}",
                channelId
            )

            // Get newest notification from database
            var previouslyLatestNotification: Notification? = db.notificationDao().latestNotification(user.user_id, user.instance_uri)

            val api = apiForUser(user, db, apiHolder)

            try {
                // Request notifications from server
                var newNotifications: List<Notification>? = api.notifications(
                    since_id = previouslyLatestNotification?.id
                )

                while (!newNotifications.isNullOrEmpty()
                    && newNotifications.map { it.created_at ?: OffsetDateTime.MIN }
                        .maxOrNull()!! > previouslyLatestNotification?.created_at ?: OffsetDateTime.MIN
                ) {
                    // Add to db
                    val filteredNewNotifications: List<Notification> = newNotifications.filter {
                        it.created_at ?: OffsetDateTime.MIN > previouslyLatestNotification?.created_at ?: OffsetDateTime.MIN
                    }.map {
                        it.copy(user_id = user.user_id, instance_uri = user.instance_uri)
                    }.sortedBy { it.created_at }

                    db.notificationDao().insertAll(filteredNewNotifications)

                    // Launch new notifications
                    filteredNewNotifications.forEach {
                        showNotification(it, user, channelId)
                    }

                    previouslyLatestNotification =
                        filteredNewNotifications.maxByOrNull { it.created_at ?: OffsetDateTime.MIN }

                    // Request again
                    newNotifications = api.notifications(
                        since_id = previouslyLatestNotification?.id
                    )
                }
            } catch (exception: IOException) {
                return Result.failure()
            } catch (exception: HttpException) {
                return Result.failure()
            }
        }

        return Result.success()
    }

    private fun showNotification(
        notification: Notification,
        user: UserDatabaseEntity,
        channelIdPrefix: String
    ) {
        val channelId = channelIdPrefix + (notification.type ?: "other").toString()

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(
                when (notification.type) {
                    follow -> R.drawable.ic_follow
                    mention -> R.drawable.mention_at_24dp
                    reblog -> R.drawable.ic_reblog
                    favourite -> R.drawable.ic_like_full
                    comment -> R.drawable.ic_comment_empty
                    poll -> R.drawable.poll
                    null -> R.drawable.ic_comment_empty
                }
            )
            .setContentTitle(
                notification.account?.username?.let { username ->
                    applicationContext.getString(
                        when (notification.type) {
                            follow -> R.string.followed_notification
                            comment -> R.string.comment_notification
                            mention -> R.string.mention_notification
                            reblog -> R.string.shared_notification
                            favourite -> R.string.liked_notification
                            poll -> R.string.poll_notification
                            null -> R.string.other_notification
                        }
                    ).format(username)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(
                PendingIntent.getActivity(applicationContext, 0, when (notification.type) {
                    mention -> notification.status?.let {
                        Intent(applicationContext, PostActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(Status.POST_TAG, notification.status)
                            putExtra(Status.VIEW_COMMENTS_TAG, true)
                        }
                    } ?: Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(SHOW_NOTIFICATION_TAG, true)
                    }
                    else -> Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(SHOW_NOTIFICATION_TAG, true)
                    }
                }, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)

        if (notification.type == mention || notification.type == comment){
            builder.setContentText(notification.status?.content)
        }
        //TODO poll -> TODO()

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification
            notify((user.instance_uri + user.user_id + notification.id).hashCode(), builder.build())
        }
    }

    private fun createNotificationChannels(handle: String, channelId: String) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the group.
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(channelId, handle))

            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val followsChannel = NotificationChannel(channelId + follow.toString(), "New followers", importance).apply { group = channelId }
            val mentionChannel = NotificationChannel(channelId + mention.toString(), "Mentions", importance).apply { group = channelId }
            val sharesChannel = NotificationChannel(channelId + reblog.toString(), "Shares", importance).apply { group = channelId }
            val likesChannel = NotificationChannel(channelId + favourite.toString(), "Likes", importance).apply { group = channelId }
            val commentsChannel = NotificationChannel(channelId + comment.toString(), "Comments", importance).apply { group = channelId }
            val pollsChannel = NotificationChannel(channelId + poll.toString(), "Polls", importance).apply { group = channelId }
            val othersChannel = NotificationChannel(channelId + "other", "Other", importance).apply { group = channelId }

            // Register the channels with the system
            notificationManager.createNotificationChannel(followsChannel)
            notificationManager.createNotificationChannel(mentionChannel)
            notificationManager.createNotificationChannel(sharesChannel)
            notificationManager.createNotificationChannel(likesChannel)
            notificationManager.createNotificationChannel(commentsChannel)
            notificationManager.createNotificationChannel(pollsChannel)
            notificationManager.createNotificationChannel(othersChannel)
        }
    }

    companion object {
        const val SHOW_NOTIFICATION_TAG = "SHOW_NOTIFICATION"
    }

}