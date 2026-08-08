package com.gostudios.console.sdk

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * Streaming client used by GoConsoleOS Link, USB Health and GoConsoleOS Cast.
 *
 * Wire format:
 *  - JSON control line terminated by `\n` (UTF-8).
 *  - Binary frames: 1 byte type + 4 byte big-endian length + payload.
 */
class LinkClient(
    private val host: ConsoleHost,
    private val listener: Listener,
) : AutoCloseable {

    interface Listener {
        fun onConnected(host: ConsoleHost)
        fun onControl(type: String, payload: JSONObject)
        fun onFrame(type: Int, payload: ByteArray)
        fun onDisconnected(error: String?)
    }

    private val running = AtomicBoolean(false)
    private val pool: ExecutorService = Executors.newSingleThreadExecutor()
    private var sock: Socket? = null
    private lateinit var out: DataOutputStream
    private var connected = false

    fun connect() {
        if (running.getAndSet(true)) return
        pool.execute { runLoop() }
    }

    private fun runLoop() {
        try {
            val s = Socket()
            sock = s
            s.connect(InetSocketAddress(host.address, host.port), 6000)
            val rawIn = BufferedInputStream(s.getInputStream())
            val rawOut = BufferedOutputStream(s.getOutputStream())
            out = DataOutputStream(rawOut)
            connected = true

            // handshake: send hello
            sendControl(Protocol.MSG_HELLO, JSONObject()
                .put("id", Protocol.MAGIC)
                .put("version", Protocol.VERSION)
                .put("client", "go-console"))
            onConnected(host)

// read loop
            val lenBuf = ByteArray(4)
            while (running.get()) {
                val first = rawIn.read()
                if (first < 0) break
                if (first == '{'.code) {
                    // control line: read until \n
                    val sb = StringBuilder()
                    sb.append('{')
                    while (true) {
                        val c = rawIn.read()
                        if (c < 0 || c == '\n'.code) break
                        sb.append(c.toChar())
                    }
                    val text = sb.toString()
                    if (text.isNotEmpty()) {
                        val obj = JSONObject(text)
                        val type = obj.optString("type", "")
                        onControl(type, obj)
                    }
                } else {
                    // binary frame: type byte already consumed
                    readFully(rawIn, header, 0, 4)
                    val len = ((header[0].toInt() and 0xFF) shl 24) or
                        ((header[1].toInt() and 0xFF) shl 16) or
                        ((header[2].toInt() and 0xFF) shl 8) or
                        (header[3].toInt() and 0xFF)
                    if (len < 0 || len > 32 * 1024 * 1024) { onDisconnected("bad frame len $len"); break }
                    val buf = ByteArray(len)
                    readFully(rawIn, buf, 0, len)
                    onFrame(first, buf)
                }
            }
        } catch (e: Exception) {
            if (running.get()) onDisconnected(e.message)
        } finally {
            connected = false
            runCatching { sock?.close() }
        }
    }

    private fun readFully(ins: java.io.InputStream, buf: ByteArray, off: Int, len: Int) {
        var o = off
        var remaining = len
        while (remaining > 0) {
            val n = ins.read(buf, o, remaining)
            if (n < 0) throw IOException("EOF")
            o += n
            remaining -= n
        }
    }

    fun sendControl(type: String, payload: JSONObject = JSONObject()) {
        if (!connected) return
        try {
            val line = (payload.put("type", type).toString() + "\n").toByteArray(StandardCharsets.UTF_8)
            synchronized(this) { out.write(line); out.flush() }
        } catch (_: Exception) {
        }
    }

    fun sendFrame(frameType: Int, payload: ByteArray) {
        if (!connected) return
        try {
            synchronized(this) {
                out.writeByte(frameType)
                out.writeInt(payload.size)
                out.write(payload)
                out.flush()
            }
        } catch (_: Exception) {
        }
    }

    fun disconnect() {
        running.set(false)
        runCatching { sock?.close() }
        pool.shutdownNow()
    }

    override fun close() = disconnect()
}