package org.pixeldroid.app.utils.notificationsWorker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

fun enablePullNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("NOTIFICATION_PULL_TAG")
        val workRequest: WorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
            .addTag("NOTIFICATION_PULL_TAG")
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        workManager.enqueue(workRequest)
    }
