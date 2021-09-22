package org.pixeldroid.app.utils.notificationsWorker

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import java.time.Instant
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
            val uniqueUserId = makeChannelGroupId(user)

            val notificationsEnabledForUser = makeNotificationChannels(
                "@${user.username}@${user.instance_uri.removePrefix("https://")}",
                uniqueUserId
            )

            //if notifications are disabled for this user, move on to next user
            if(!notificationsEnabledForUser) continue

            // Get newest notification from database
            var previouslyLatestNotification: Notification? = db.notificationDao().latestNotification(user.user_id, user.instance_uri)

            val api = apiForUser(user, db, apiHolder)

            try {
                // Request notifications from server
                var newNotifications: List<Notification>? = api.notifications(
                    since_id = previouslyLatestNotification?.id
                )

                while (!newNotifications.isNullOrEmpty()
                    && newNotifications.map { it.created_at ?: Instant.MIN }
                        .maxOrNull()!! > previouslyLatestNotification?.created_at ?: Instant.MIN
                ) {
                    // Add to db
                    val filteredNewNotifications: List<Notification> = newNotifications.filter {
                        it.created_at ?: Instant.MIN > previouslyLatestNotification?.created_at ?: Instant.MIN
                    }.map {
                        it.copy(user_id = user.user_id, instance_uri = user.instance_uri)
                    }.sortedBy { it.created_at }

                    db.notificationDao().insertAll(filteredNewNotifications)

                    // Launch new notifications
                    filteredNewNotifications.forEach {
                        showNotification(it, user, uniqueUserId)
                    }

                    previouslyLatestNotification =
                        filteredNewNotifications.maxByOrNull { it.created_at ?: Instant.MIN }

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
        uniqueUserId: String
    ) {
        val intent: Intent = when (notification.type) {
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
        }.putExtra(USER_NOTIFICATION_TAG, user.user_id)
            .putExtra(INSTANCE_NOTIFICATION_TAG, user.instance_uri)


        val builder = NotificationCompat.Builder(applicationContext, makeChannelId(uniqueUserId, notification.type))
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
            .setColor(Color.parseColor("#6200EE"))
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
                PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .setAutoCancel(true)

        if (notification.type == mention || notification.type == comment || notification.type == poll){
            builder.setContentText(notification.status?.content)
        }

        builder.setGroup(uniqueUserId)

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification
            notify((uniqueUserId + notification.id).hashCode(), builder.build())
        }
    }

    private fun makeNotificationChannels(handle: String, channelGroupId: String): Boolean {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the group, hashed (since when creating the group, it may be truncated if too long)
            val hashedGroupId = channelGroupId.hashCode().toString()
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(hashedGroupId, handle))

            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channels: List<NotificationChannel> = listOf(
                NotificationChannel(makeChannelId(channelGroupId, follow), applicationContext.getString(R.string.followed_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, mention), applicationContext.getString(R.string.mention_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, reblog), applicationContext.getString(R.string.shared_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, favourite), applicationContext.getString(R.string.liked_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, comment), applicationContext.getString(R.string.comment_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, poll), applicationContext.getString(R.string.poll_notification_channel), importance),
                NotificationChannel(makeChannelId(channelGroupId, null), applicationContext.getString(R.string.other_notification_channel), importance),
            ).map {
                it.apply { group = hashedGroupId }
            }

            // Register the channels with the system
            notificationManager.createNotificationChannels(channels)

            //Return true if notifications are enabled, false if disabled
            return notificationManager.areNotificationsEnabled() and
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val channelGroup =
                            notificationManager.getNotificationChannelGroup(hashedGroupId)
                        !channelGroup.isBlocked
                    } else true) and
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        !notificationManager.areNotificationsPaused()
                    } else true) and
                    !channels.all {
                        notificationManager.getNotificationChannel(it.id).importance <= NotificationManager.IMPORTANCE_NONE
                    }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    companion object {
        const val SHOW_NOTIFICATION_TAG = "org.pixeldroid.app.SHOW_NOTIFICATION"
        const val INSTANCE_NOTIFICATION_TAG = "org.pixeldroid.app.USER_NOTIFICATION"
        const val USER_NOTIFICATION_TAG = "org.pixeldroid.app.INSTANCE_NOTIFICATION"

        const val otherNotificationType = "other"
    }

}

/**
 * [channelGroupId] is the id used to uniquely identify the group: for us it is a unique id
 * identifying a user consisting of the concatenation of the instance uri and user id.
 */
private fun makeChannelId(channelGroupId: String, type: Notification.NotificationType?): String =
    (channelGroupId + (type ?: NotificationsWorker.otherNotificationType)).hashCode().toString()

private fun makeChannelGroupId(user: UserDatabaseEntity) = user.instance_uri + user.user_id


fun removeNotificationChannelsFromAccount(context: Context, user: UserDatabaseEntity?) = user?.let {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelGroupId = makeChannelGroupId(user)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            notificationManager.deleteNotificationChannelGroup(channelGroupId.hashCode().toString())
        } else {
            val types: MutableList<Notification.NotificationType?> =
                Notification.NotificationType.values().toMutableList()
            types += null

            types.forEach {
                notificationManager.deleteNotificationChannel(makeChannelId(channelGroupId, it))
            }
        }
    }
}