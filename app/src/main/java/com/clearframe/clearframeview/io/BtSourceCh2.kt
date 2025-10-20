package com.clearframe.clearframeview.io

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import java.io.InputStream
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

class BtSourceCh2(
    private val deviceAddress: String,
    private val scope: CoroutineScope
) : IBinarySource {

    private var job: Job? = null
    private var socket: BluetoothSocket? = null

    override fun start(onFrame: (ByteArray) -> Unit) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            val dev = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(deviceAddress)
            requireNotNull(dev) { "BT device not found: " }
            connectAndRead(dev, onFrame)
        }
    }

    private fun createChannelSocket(dev: BluetoothDevice, channel: Int): BluetoothSocket {
        val m: Method = dev.javaClass.getMethod("createRfcommSocket", Integer.TYPE)
        @Suppress("UNCHECKED_CAST")
        return m.invoke(dev, channel) as BluetoothSocket
    }

    private fun connectAndRead(dev: BluetoothDevice, onFrame: (ByteArray) -> Unit) {
        while (job?.isActive == true) {
            try {
                socket = createChannelSocket(dev, 2) // fixed RFCOMM ch2
                socket!!.connect()
                val ins = socket!!.inputStream
                readLoop(ins, onFrame)
            } catch (_: Throwable) {
                closeQuietly()
                Thread.sleep(750)
            }
        }
    }

    private fun readLoop(ins: InputStream, onFrame: (ByteArray) -> Unit) {
        val MAGIC = byteArrayOf(0x43,0x46,0x30,0x31) // "CF01"
        val hdr = ByteArray(12)
        while (job?.isActive == true) {
            if (!readFully(ins, hdr, 0, 4)) break
            if (!hdr.copyOfRange(0,4).contentEquals(MAGIC)) continue
            if (!readFully(ins, hdr, 4, 8)) break
            val bb = ByteBuffer.wrap(hdr, 4, 8).order(ByteOrder.LITTLE_ENDIAN)
            val len = bb.int
            val wantCrc = bb.int
            if (len <= 0 || len > (8 shl 20)) continue
            val payload = ByteArray(len)
            if (!readFully(ins, payload, 0, len)) break
            val c = CRC32().apply { update(payload) }.value.toInt()
            if (c == wantCrc) onFrame(payload)
        }
    }

    private fun readFully(ins: InputStream, buf: ByteArray, off: Int, len: Int): Boolean {
        var n = 0
        while (n < len) {
            val r = ins.read(buf, off + n, len - n)
            if (r <= 0) return false
            n += r
        }
        return true
    }

    override fun stop() {
        job?.cancel()
        closeQuietly()
    }

    private fun closeQuietly() = try { socket?.close() } catch (_: Throwable) {}
}