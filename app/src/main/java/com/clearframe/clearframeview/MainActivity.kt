package com.clearframe.clearframeview

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var button: Button

    private val TAG = "CFBlade"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.textViewStatus)
        button = findViewById(R.id.buttonAction)

        Toast.makeText(this, "App started", Toast.LENGTH_SHORT).show()
        status.text = "Blade app started. Checking Bluetooth…"
        Log.i(TAG, "onCreate: starting")

        ensurePermissionsIfNeeded()

        button.setOnClickListener { connectToAlias("CF-RX", manual = true) }
    }

    override fun onResume() {
        super.onResume()
        // Auto-connect on resume
        connectToAlias("CF-RX", manual = false)
    }

    private fun connectToAlias(alias: String, manual: Boolean) {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) {
            val msg = "Bluetooth not available/enabled"
            status.text = msg
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            Log.e(TAG, msg)
            return
        }

        // List bonded devices in the UI so you can verify pairing
        val bonded = adapter.bondedDevices?.toList() ?: emptyList()
        val list = if (bonded.isEmpty()) "(none)" else bonded.joinToString { "${it.name}[${it.address}]" }
        status.text = "Bonded: $list"
        Log.i(TAG, "Bonded devices: $list")

        val device = bonded.firstOrNull { it.name == alias }
        if (device == null) {
            val msg = "Paired device '$alias' not found. Pair in Settings → Bluetooth."
            status.text = msg + "\nBonded: $list"
            Log.e(TAG, msg)
            if (manual) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return
        }

        status.text = "Connecting to ${device.name}…"
        button.isEnabled = false
        Log.i(TAG, "Connecting to ${device.name} (${device.address})")

        thread {
            var sock: BluetoothSocket? = null
            try {
                // Try secure first
                sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter.cancelDiscovery()
                sock.connect()
                readLoop(sock)
            } catch (e1: Exception) {
                Log.w(TAG, "Secure connect failed: ${e1.message}")
                try {
                    // Fallback: insecure
                    val insecure = device.javaClass
                        .getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
                        .invoke(device, SPP_UUID) as BluetoothSocket
                    (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter.cancelDiscovery()
                    insecure.connect()
                    readLoop(insecure)
                } catch (e2: Exception) {
                    Log.e(TAG, "Insecure connect failed: ${e2.message}", e2)
                    runOnUiThread {
                        status.text = "Connect failed: ${e2.message ?: "unknown"}"
                        button.isEnabled = true
                        Toast.makeText(this, "Connect failed", Toast.LENGTH_SHORT).show()
                    }
                    try { sock?.close() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun readLoop(sock: BluetoothSocket) {
        runOnUiThread {
            status.text = "Connected. Reading…"
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
        }
        try {
            val reader = BufferedReader(InputStreamReader(sock.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                Log.d(TAG, "RX: $line")
                runOnUiThread { status.text = line }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Read error: ${e.message}")
            runOnUiThread { status.text = "Link closed: ${e.message}" }
        } finally {
            try { sock.close() } catch (_: Exception) {}
            runOnUiThread { button.isEnabled = true }
        }
    }

    private fun ensurePermissionsIfNeeded() {
        // On Android 6.x (some Blades), ACCESS_COARSE_LOCATION is runtime for BT discovery
        val need = listOf(Manifest.permission.ACCESS_COARSE_LOCATION).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 42)
            Log.i(TAG, "Requesting permissions: $need")
        }
    }
}
