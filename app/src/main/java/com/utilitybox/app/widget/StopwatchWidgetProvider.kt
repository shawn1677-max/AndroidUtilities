package com.utilitybox.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.utilitybox.app.MainActivity
import com.utilitybox.app.R
import com.utilitybox.app.tools.ToolIds
import com.utilitybox.app.util.StopwatchState
import com.utilitybox.app.util.StopwatchStore
import com.utilitybox.app.util.formatClock

private const val ACTION_TOGGLE = "com.utilitybox.app.widget.STOPWATCH_TOGGLE"
private const val ACTION_RESET = "com.utilitybox.app.widget.STOPWATCH_RESET"

/**
 * A home screen stopwatch sharing its state with the in-app tool through
 * [StopwatchStore], so the two are one timer rather than two that disagree.
 *
 * A widget cannot be redrawn once a second, so the running reading is drawn by
 * a [android.widget.Chronometer], which ticks itself inside the launcher with
 * no app process involved. Only start, pause and reset cost a redraw.
 */
class StopwatchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = StopwatchStore.read(context)
        appWidgetIds.forEach { id -> render(context, appWidgetManager, id, state) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> renderAll(context, StopwatchStore.toggle(context))
            ACTION_RESET -> renderAll(context, StopwatchStore.reset(context))
            else -> super.onReceive(context, intent)
        }
    }

    companion object {

        /** Redraws every placed widget, for callers that changed the shared state. */
        fun refresh(context: Context) {
            renderAll(context, StopwatchStore.read(context))
        }

        private fun renderAll(context: Context, state: StopwatchState) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, StopwatchWidgetProvider::class.java)
            val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull() ?: return
            ids.forEach { id -> render(context, manager, id, state) }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            state: StopwatchState,
        ) {
            val now = SystemClock.elapsedRealtime()
            val views = RemoteViews(context.packageName, R.layout.widget_stopwatch).apply {
                if (state.running) {
                    setChronometer(
                        R.id.widget_chronometer,
                        state.chronometerBase(now),
                        null,
                        true,
                    )
                    setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    setViewVisibility(R.id.widget_static_time, View.GONE)
                } else {
                    // Stop the chronometer as well as hiding it, so a hidden view
                    // is not left ticking behind the static reading.
                    setChronometer(R.id.widget_chronometer, now, null, false)
                    setViewVisibility(R.id.widget_chronometer, View.GONE)
                    setTextViewText(R.id.widget_static_time, formatClock(state.accumulatedMs))
                    setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                }

                setImageViewResource(
                    R.id.widget_toggle,
                    if (state.running) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                )
                setContentDescription(
                    R.id.widget_toggle,
                    context.getString(
                        if (state.running) {
                            R.string.widget_stopwatch_pause
                        } else {
                            R.string.widget_stopwatch_start
                        }
                    ),
                )

                setOnClickPendingIntent(R.id.widget_toggle, broadcast(context, ACTION_TOGGLE, 1))
                setOnClickPendingIntent(R.id.widget_reset, broadcast(context, ACTION_RESET, 2))
                // Tapping the reading opens the full tool, where the laps live.
                setOnClickPendingIntent(R.id.widget_readout, openStopwatch(context))
            }
            runCatching { manager.updateAppWidget(widgetId, views) }
        }

        private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, StopwatchWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openStopwatch(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(MainActivity.EXTRA_OPEN_TOOL, ToolIds.STOPWATCH)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
