package com.clearframe.clearframeview

import android.app.*
import android.bluetooth.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class SppKeepaliveService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        private const val TAG = "CF.SPP"
        private const val CHANNEL_ID = "cf_spp"
        private const val NOTIFY_ID = 101
        // Standard SPP UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        // Pi (CF-RX) MAC — CHANGE IF DIFFERENT
        const val CF_RX_MAC = "98:DA:92:01:0A:31"
        // Heartbeat seconds
        const val HB_SEC = 10L
    }

    private lateinit var wake: PowerManager.WakeLock
    private var sock: BluetoothSocket? = null
    private var ins: InputStream? = null
    private var outs: OutputStream? = null
    @Volatile private var running = false

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFY_ID, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClearFrame Link")
            .setContentText("Maintaining SPP connection to CF-RX")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build())

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CF:SppWakelock").apply {
            setReferenceCounted(false); acquire()
        }

        registerReceiver(btRx, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        })

        running = true
        Thread { loop() }.start()
    }

    override fun onDestroy() {
        running = false
        try { unregisterReceiver(btRx) } catch (_:Exception){}
        closeSock()
        if (this::wake.isInitialized && wake.isHeld) wake.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private val btRx = object: BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val d: BluetoothDevice? = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (d?.address.equals(CF_RX_MAC, true)) {
                        Log.w(TAG, "ACL disconnected; will reconnect")
                        closeSock()
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> { /* no-op */ }
            }
        }
    }

    private fun loop() {
        var backoff = 1_000L
        while (running) {
            try {
                ensureConnected()
                backoff = 1_000L

                // Heartbeat (keeps link active, triggers re-connect on failure)
                while (running && sock?.isConnected == true) {
                    try {
                        outs?.write("PING\n".toByteArray())
                        outs?.flush()
                    } catch (e: Exception) {
                        Log.w(TAG, "Write failed: ${e.message}")
                        closeSock()
                        break
                    }
                    Thread.sleep(HB_SEC * 1000)
                }
            } catch (e: Exception) {
                closeSock()
            }
            if (!running) break
            try { Thread.sleep(backoff) } catch (_:Exception){}
            backoff = (backoff * 2).coerceAtMost(20_000L) // 1s → 20s
        }
    }

    private fun ensureConnected() {
        if (sock?.isConnected == true) return
        val ad = BluetoothAdapter.getDefaultAdapter()
        require(ad?.isEnabled == true) { "Bluetooth adapter off" }
        val dev = ad.getRemoteDevice(CF_RX_MAC)

        // Secure first, then fall back to insecure
        try {
            sock = dev.createRfcommSocketToServiceRecord(SPP_UUID)
            sock!!.connect()
        } catch (e: Exception) {
            try { sock?.close() } catch (_:Exception){}
            sock = dev.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            sock!!.connect()
        }
        ins = sock!!.inputStream
        outs = sock!!.outputStream
        Log.i(TAG, "SPP connected to ${dev.address}")
    }

    private fun closeSock() {
        try { ins?.close() } catch (_:Exception){}
        try { outs?.close() } catch (_:Exception){}
        try { sock?.close() } catch (_:Exception){}
        ins = null; outs = null; sock = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "ClearFrame Link", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }
}

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "cf_spp_keepalive"
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(chId, "ClearFrame SPP", NotificationManager.IMPORTANCE_LOW)
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
        try { if (this::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release() } catch (_: Throwable) {}
        super.onDestroy()
    }