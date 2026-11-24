package nl.ritbon.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val UPDATE_NOTIFICATION_WORK = "vlotrit_update_notification_check"

internal fun scheduleUpdateNotificationChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<UpdateNotificationWorker>(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
        UPDATE_NOTIFICATION_WORK,
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

class UpdateNotificationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("setting_update_notifications", false)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        return try {
            val currentVersion = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0"
            val release = withContext(Dispatchers.IO) {
                fetchLatestRelease("redglowmk3/vlotrit", currentVersion)
            }
            if (compareVersionNames(release.versionName, currentVersion) > 0) {
                postUpdateAvailableNotification(applicationContext, prefs, release)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
