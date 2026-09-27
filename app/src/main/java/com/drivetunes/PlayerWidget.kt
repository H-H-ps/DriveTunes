package com.drivetunes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/** Home-screen widget: shows the current song and lets you play/pause/skip without opening the app. */
class PlayerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.drivetunes.widget.TOGGLE"
        const val ACTION_NEXT = "com.drivetunes.widget.NEXT"
        const val ACTION_PREV = "com.drivetunes.widget.PREV"

        /** Called by PlaybackService whenever playback state changes, so every placed widget refreshes. */
        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(ctx)
            for (id in ids) mgr.updateAppWidget(id, views)
        }

        private fun buildViews(ctx: Context): RemoteViews {
            val views = RemoteViews(ctx.packageName, R.layout.widget_player)
            val playing = Prefs.nowPlaying(ctx)
            val title = Prefs.nowTitle(ctx)
            val artist = Prefs.nowArtist(ctx)
            views.setTextViewText(R.id.widget_title, title.ifEmpty { ctx.getString(R.string.app_name) })
            views.setTextViewText(R.id.widget_artist, artist.ifEmpty { ctx.getString(R.string.widget_idle) })
            views.setImageViewResource(
                R.id.widget_play,
                if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            views.setOnClickPendingIntent(R.id.widget_play, actionIntent(ctx, ACTION_TOGGLE, 1))
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(ctx, ACTION_NEXT, 2))
            views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(ctx, ACTION_PREV, 3))

            // Tapping the art or text opens the app.
            val openApp = PendingIntent.getActivity(
                ctx, 4, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_art, openApp)
            views.setOnClickPendingIntent(R.id.widget_title, openApp)
            views.setOnClickPendingIntent(R.id.widget_artist, openApp)
            return views
        }

        private fun actionIntent(ctx: Context, action: String, code: Int): PendingIntent {
            val i = Intent(ctx, PlayerWidget::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                ctx, code, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(ctx)
        for (id in appWidgetIds) mgr.updateAppWidget(id, views)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val serviceAction = when (intent.action) {
            ACTION_TOGGLE -> PlaybackService.ACTION_TOGGLE
            ACTION_NEXT -> PlaybackService.ACTION_NEXT
            ACTION_PREV -> PlaybackService.ACTION_PREV
            else -> null
        } ?: return
        ContextCompat.startForegroundService(
            ctx,
            Intent(ctx, PlaybackService::class.java).setAction(serviceAction)
        )
    }
}
