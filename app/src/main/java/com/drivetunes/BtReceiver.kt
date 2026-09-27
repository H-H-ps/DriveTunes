package com.drivetunes

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/** Fires when any Bluetooth device connects; starts playback if it is one of the chosen devices. */
class BtReceiver : BroadcastReceiver() {

    companion object {
        private var lastStart = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        // Auto-play switched off entirely: this event fires for every Bluetooth device on the
        // phone (headphones, watch, etc.), so skip logging here to keep the log from filling up.
        if (!Prefs.autoPlay(context)) return

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val address = device?.address
        if (device == null || address == null) {
            AutoPlayLog.addError(context, "وصل حدث اتصال بلوتوث بدون معلومات جهاز (تجاهل)")
            return
        }
        val name = try {
            device.name
        } catch (e: SecurityException) {
            null
        }
        // Not one of the devices chosen in Settings -> expected/frequent, no need to log.
        if (address !in Prefs.devices(context)) return

        // The system often sends several ACL events for one connection
        val now = System.currentTimeMillis()
        if (now - lastStart < 15_000) {
            AutoPlayLog.add(context, "اتصال متكرر من ${name ?: address} خلال ١٥ ثانية (تجاهل)")
            return
        }
        lastStart = now

        val delayMs = Prefs.delay(context) * 1000L
        AutoPlayLog.add(context, "اتصل الجهاز ${name ?: address}، بدء الخدمة (تأخير التشغيل الفعلي ${Prefs.delay(context)} ثانية)")
        val app = context.applicationContext
        // Start the foreground service immediately instead of waiting here: a bare
        // BroadcastReceiver has no elevated process priority and the system can kill it (and the
        // whole process) before a delayed callback ever runs, especially with the screen off.
        // Once startForeground() has been called the process is far harder to kill, so the
        // configurable delay itself is applied inside the service (see PlaybackService), not here.
        try {
            ContextCompat.startForegroundService(
                app,
                Intent(app, PlaybackService::class.java)
                    .setAction(PlaybackService.ACTION_AUTO_PLAY)
                    .putExtra(PlaybackService.EXTRA_DELAY_MS, delayMs)
            )
        } catch (e: Exception) {
            AutoPlayLog.addError(
                app,
                "فشل تشغيل خدمة التشغيل التلقائي (${e.javaClass.simpleName}: ${e.message}), عرض إشعار بديل"
            )
            Notifier.tapToPlay(app, name)
        }
    }
}
