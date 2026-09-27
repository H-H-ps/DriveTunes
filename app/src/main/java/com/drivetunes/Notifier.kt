package com.drivetunes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CH_PLAYBACK = "drivetunes_playback"
    const val CH_TAP = "drivetunes_tap"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_PLAYBACK, ctx.getString(R.string.notif_ch_playback), NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TAP, ctx.getString(R.string.notif_ch_tap), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    /** Fallback when Android refuses to start the playback service from the background. */
    fun tapToPlay(ctx: Context, deviceName: String?) {
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_AUTO_PLAY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(
            ctx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CH_TAP)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(ctx.getString(R.string.notif_tap_title, deviceName ?: ""))
            .setContentText(ctx.getString(R.string.notif_tap_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(2001, n)
        } catch (e: SecurityException) {
        }
    }
}
