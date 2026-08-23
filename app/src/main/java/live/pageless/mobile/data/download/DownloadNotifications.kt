package live.pageless.mobile.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import live.pageless.mobile.R

/**
 * Builds download progress/completion notifications and owns the notification
 * channel. The progress notification backs the foreground download worker; the
 * completion/failure notification is a separate, dismissible one-shot.
 */
object DownloadNotifications {
    const val CHANNEL_ID = "downloads"

    /** Stable id for the ongoing foreground-progress notification per book. */
    fun progressNotificationId(bookId: String): Int = bookId.hashCode()

    /** Id for the terminal (completed/failed) notification per book. */
    private fun completionNotificationId(bookId: String): Int = bookId.hashCode() xor 0x7000_0000

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Audiobook download progress" },
            )
        }
    }

    fun progress(
        context: Context,
        title: String,
        percent: Int?,
    ): Notification {
        ensureChannel(context)
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle("Downloading")
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_stat_download)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (percent != null && percent >= 0) {
            builder.setProgress(100, percent, false).setContentText("$title  •  $percent%")
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    fun completed(
        context: Context,
        bookId: String,
        title: String,
    ) = notifyTerminal(context, bookId, "Download complete", title)

    fun failed(
        context: Context,
        bookId: String,
        title: String,
    ) = notifyTerminal(context, bookId, "Download failed", title)

    /**
     * Dismisses every download notification this app has posted.
     *
     * For account teardown. A "Download complete" notification names the book,
     * is dismissible rather than self-clearing, and outlives sign-out — leaving
     * the previous account's library titles in the shade for whoever picks the
     * device up next.
     *
     * Filters by channel rather than calling `cancelAll()`, so the media
     * notification, which Media3 owns and posts on its own channel, is left for
     * the player to tear down.
     */
    fun cancelAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.activeNotifications
            .filter { it.notification.channelId == CHANNEL_ID }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    private fun notifyTerminal(
        context: Context,
        bookId: String,
        heading: String,
        title: String,
    ) {
        ensureChannel(context)
        val manager = context.getSystemService<NotificationManager>() ?: return
        val n =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(heading)
                .setContentText(title)
                .setSmallIcon(R.drawable.ic_stat_download)
                .setAutoCancel(true)
                .build()
        manager.notify(completionNotificationId(bookId), n)
    }
}
