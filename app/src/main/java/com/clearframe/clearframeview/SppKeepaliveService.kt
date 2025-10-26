package com.clearframe.clearframeview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that holds a PARTIAL_WAKE_LOCK so CPU stays awake
 * while the Bluetooth SPP link is active. Screen is kept on by the Activity
 * via FLAG_KEEP_SCREEN_ON; this service focuses on CPU/network.
 */
class SppKeepaliveService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        acquireCpuWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If you later add timers/keepalives, start them here.
        // Keep running unless explicitly stopped.
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "cf_spp_keepalive"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                chId,
                "ClearFrame SPP",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(ch)
        }
        val note: Notification = NotificationCompat.Builder(this, chId)
            .setContentTitle("ClearFrame Link")
            .setContentText("Bluetooth SPP connected")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
        startForeground(1001, note)
    }

    private fun acquireCpuWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClearFrame:SPP")
        wakeLock.setReferenceCounted(false)
        if (!wakeLock.isHeld) wakeLock.acquire()
    }

    override fun onDestroy() {
        try {
            if (this::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
        } catch (_: Throwable) { /* ignore */ }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}