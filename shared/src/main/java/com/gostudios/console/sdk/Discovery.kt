package com.gostudios.console.sdk

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * Discovers GoConsoleOS hosts on the local LAN using a UDP beacon.
 */
object Discovery {
    private const val BROADCAST = "255.255.255.255"

    /**
     * Broadcasts a "hello" once and listens for `GCS` replies.
     * Implementations of [onHost] are called on a background thread as hosts reply.
     */
    class Scanner(private val onHost: (ConsoleHost) -> Unit) : AutoCloseable {
        private val running = AtomicBoolean(false)
        private val executor: ExecutorService = Executors.newSingleThreadExecutor()
        private var sock: DatagramSocket? = null

        fun start() {
            if (running.getAndSet(true)) return
            executor.execute {
                try {
                    sock = DatagramSocket().apply { broadcast = true; soTimeout = 1500 }
                    val hello = JSONObject()
                        .put("kind", "hello")
                        .put("app", Protocol.MAGIC)
                        .put("version", Protocol.VERSION)
                        .toString().toByteArray(StandardCharsets.UTF_8)
                    sock!!.send(DatagramPacket(hello, hello.size, InetAddress.getByName(BROADCAST), Protocol.DISCOVERY_PORT))
                    val buf = ByteArray(2048)
                    var lastPing = 0L
                    while (running.get()) {
                        val now = System.currentTimeMillis()
                        if (now - lastPing > 2000) {
                            sock!!.send(DatagramPacket(hello, hello.size, InetAddress.getByName(BROADCAST), Protocol.DISCOVERY_PORT))
                            lastPing = now
                        }
                        try {
                            val pkt = DatagramPacket(buf, buf.size)
                            sock!!.receive(pkt)
                            val text = String(buf, 0, pkt.length, StandardCharsets.UTF_8)
                            val obj = JSONObject(text)
                            if (obj.optString("id") == Protocol.MAGIC || obj.optString("kind") == "console.os") {
                                val host = ConsoleHost.fromJson(obj.put("address", pkt.address.hostAddress))
                                onHost(host)
                            }
                        } catch (_: java.net.SocketTimeoutException) {
                            // no reply this round, keep scanning
                        } catch (_: Exception) {
                            if (running.get()) Thread.sleep(100)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { sock?.close() }
                }
            }
        }

        fun stop() {
            running.set(false)
            runCatching { sock?.close() }
            executor.shutdownNow()
        }

        override fun close() = stop()
    }
}