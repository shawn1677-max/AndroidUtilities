package com.utilitybox.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.edit
import com.utilitybox.app.R
import com.utilitybox.app.util.torchCameraId
import java.util.concurrent.atomic.AtomicBoolean

private const val ACTION_TOGGLE = "com.utilitybox.app.widget.TOGGLE_TORCH"
private const val PREFS = "utilitybox_widget"
private const val KEY_TORCH_ON = "torch_on"

/**
 * A one-cell home screen widget that toggles the camera torch.
 *
 * The torch is a shared system resource: the in-app tool, the quick settings
 * tile and other apps can all change it. Rather than trusting a remembered
 * flag, a tap reads the real state from [CameraManager.TorchCallback] — which
 * reports the current mode as soon as it is registered — and toggles from
 * that. A stale widget therefore self-corrects on the first tap instead of
 * appearing to do nothing.
 */
class FlashlightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val enabled = readStoredState(context)
        appWidgetIds.forEach { id -> render(context, appWidgetManager, id, enabled) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            toggleTorch(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun toggleTorch(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraId = cameraManager?.torchCameraId()
        if (cameraManager == null || cameraId == null) {
            storeState(context, false)
            renderAll(context)
            return
        }

        // The camera callback is asynchronous, so keep the broadcast alive.
        val pendingResult = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val settled = AtomicBoolean(false)
        var callback: CameraManager.TorchCallback? = null

        fun settle(newState: Boolean?) {
            if (!settled.compareAndSet(false, true)) return
            callback?.let { runCatching { cameraManager.unregisterTorchCallback(it) } }
            if (newState != null) storeState(context, newState)
            renderAll(context)
            pendingResult.finish()
        }

        callback = object : CameraManager.TorchCallback() {
            private var acted = false

            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                // Registration reports the current mode; ignore the echo of our own write.
                if (id != cameraId || acted) return
                acted = true
                val target = !enabled
                val applied = try {
                    cameraManager.setTorchMode(cameraId, target)
                    target
                } catch (error: CameraAccessException) {
                    // Another app holds the camera; leave the state as we found it.
                    enabled
                } catch (error: IllegalArgumentException) {
                    enabled
                }
                handler.post { settle(applied) }
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id != cameraId || acted) return
                acted = true
                handler.post { settle(false) }
            }
        }

        runCatching { cameraManager.registerTorchCallback(callback, handler) }
            .onFailure { settle(null) }

        // Safety net: never leave the broadcast hanging if no callback arrives.
        handler.postDelayed({ settle(null) }, CALLBACK_TIMEOUT_MS)
    }

    companion object {
        private const val CALLBACK_TIMEOUT_MS = 2_000L

        /**
         * Keeps the widget in step when the torch is changed from inside the app.
         */
        fun onTorchStateChanged(context: Context, enabled: Boolean) {
            storeState(context, enabled)
            renderAll(context)
        }

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        private fun readStoredState(context: Context): Boolean =
            prefs(context).getBoolean(KEY_TORCH_ON, false)

        private fun storeState(context: Context, enabled: Boolean) {
            prefs(context).edit { putBoolean(KEY_TORCH_ON, enabled) }
        }

        private fun renderAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, FlashlightWidgetProvider::class.java)
            val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull() ?: return
            val enabled = readStoredState(context)
            ids.forEach { id -> render(context, manager, id, enabled) }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            enabled: Boolean,
        ) {
            val hasFlash = (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
                ?.torchCameraId() != null

            val views = RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
                setImageViewResource(
                    R.id.widget_icon,
                    if (enabled && hasFlash) {
                        R.drawable.ic_widget_flashlight_on
                    } else {
                        R.drawable.ic_widget_flashlight_off
                    },
                )
                setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    if (enabled && hasFlash) {
                        R.drawable.widget_background_on
                    } else {
                        R.drawable.widget_background_off
                    },
                )
                setContentDescription(
                    R.id.widget_root,
                    context.getString(
                        when {
                            !hasFlash -> R.string.widget_flashlight_unavailable
                            enabled -> R.string.widget_flashlight_on
                            else -> R.string.widget_flashlight_off
                        }
                    ),
                )
                if (hasFlash) {
                    setOnClickPendingIntent(R.id.widget_root, togglePendingIntent(context))
                }
            }
            runCatching { manager.updateAppWidget(widgetId, views) }
        }

        private fun togglePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, FlashlightWidgetProvider::class.java)
                .setAction(ACTION_TOGGLE)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
