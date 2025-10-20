package com.clearframe.clearframeview.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.clearframe.clearframeview.io.BtSourceCh2
import com.clearframe.clearframeview.io.IBinarySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CFBtTestActivity : ComponentActivity() {

    private val bladeMac = "98:DA:92:01:0A:31"
    private var frames = 0
    private var source: IBinarySource? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this).apply {
            text = "CF-RX Bluetooth test: waiting…"
            textSize = 18f
            setPadding(24, 48, 24, 48)
        }
        setContentView(tv)

        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ), 1)
        }

        val s = BtSourceCh2(bladeMac, lifecycleScope)
        source = s
        s.start { bytes: ByteArray ->
            lifecycleScope.launch {
                frames += 1
                withContext(Dispatchers.Main) {
                    tv.text = "Connected to CF-RX\nFrames received: \nLast size:  bytes"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        source?.stop()
    }
}