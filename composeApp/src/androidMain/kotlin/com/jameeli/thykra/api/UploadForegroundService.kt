package com.jameeli.thykra.api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jameeli.thykra.MainActivity
import com.jameeli.thykra.R

/**
 * Keeps an upload batch alive while the app is in the background.
 *
 * Without this, Android is free to freeze the process the moment someone leaves the app,
 * and a 43-file batch stops halfway with no explanation. The notification is the price of
 * that permission and is also the honest thing to show: work is happening on the person's
 * behalf, off screen.
 *
 * It renders what it is told and nothing more — [UploadNotifier] owns the queue and
 * pushes progress in. Keeping the queue out of the service means the service cannot
 * disagree with the dock about what is happening.
 *
 * Strings come from Android resources rather than the Compose string table because this
 * is platform chrome drawn outside composition, which is the same reason the widgets use
 * them. Android resolves values-ar for them on its own.
 */
class UploadForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val done = intent?.getIntExtra(EXTRA_DONE, 0) ?: 0
        val total = intent?.getIntExtra(EXTRA_TOTAL, 0) ?: 0

        if (total <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NotificationId, buildNotification(done, total))

        // NOT_STICKY: if the process is killed, WorkManager's UploadWorker is already the
        // safety net that resumes the queue. Restarting this service with a null intent
        // would only put up a notification with nothing behind it.
        return START_NOT_STICKY
    }

    private fun buildNotification(done: Int, total: Int): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ChannelId,
                    getString(R.string.upload_channel_name),
                    // LOW: no sound, no heads-up. Uploading is not an interruption.
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = getString(R.string.upload_channel_description) },
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, ChannelId)
            .setContentTitle(getString(R.string.upload_notification_title))
            .setContentText(getString(R.string.upload_notification_progress, done, total))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(total, done, false)
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // The count is the whole content; showing a timestamp invites the reading
            // that something happened at a moment rather than is happening now.
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val ChannelId = "thykra_uploads"
        private const val NotificationId = 4201
        const val EXTRA_DONE = "done"
        const val EXTRA_TOTAL = "total"
    }
}
