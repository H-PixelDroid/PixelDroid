package org.pixeldroid.app.utils.notificationsWorker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

fun enablePullNotifications(context: Context) {
    val workManager = WorkManager.getInstance(context)
    val tag = "NOTIFICATION_PULL_TAG"
    workManager.cancelAllWorkByTag(tag)
    val workRequest: WorkRequest = PeriodicWorkRequestBuilder<NotificationsWorker>(
        PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
        PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
    )
        .addTag(tag)
        .setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        )
        .build()
    workManager.enqueue(workRequest)
}
