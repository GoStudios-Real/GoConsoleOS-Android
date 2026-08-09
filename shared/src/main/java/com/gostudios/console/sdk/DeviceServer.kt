package com.gostudios.console.sdk

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * On-device server for GoConsoleOS Portable. Serves the ACC account API and a
 * small GoAI assistant over HTTP, so the phone/tablet/TV itself can act as a
 * GoConsoleOS host - mirroring the server built into the USB console.
 *
 * Uses a plain [ServerSocket] with a tiny HTTP parser so it works on every
 * Android API level with zero extra dependencies.
 *
 * Port: 39210 (matches the desktop GoConsoleOS server).
 */
object DeviceServer {
    const val PORT = 39210

    private var socket: ServerSocket? = null
    private var acceptor: Thread? = null
    private var pool = Executors.newCachedThreadPool()
    private var store: AccStore? = null
    private var goAi: GoAiLocal? = null

    @Volatile
    var running: Boolean = false
        private set

    fun start(context: Context) {
        if (running) return
        try {
            store = AccStore(context)
            goAi = GoAiLocal()

            val s = ServerSocket(PORT, 8, InetAddress.getByName("0.0.0.0"))
            socket = s
            running = true

            val t = Thread { acceptLoop(s) }
            t.isDaemon = true
            t.name = "go-console-device-server"
            acceptor = t
            t.start()
        } catch (e: Exception) {
            running = false
            socket = null
        }
    }

    fun stop() {
        running = false
        try { socket?.close() } catch (e: Exception) {}
        socket = null
        acceptor = null
    }

    private fun acceptLoop(server: ServerSocket) {
        while (running) {
            try {
                val client = server.accept()
                pool.execute { handleConnection(client) }
            } catch (e: Exception) {
                if (!running) break
            }
        }
    }

    private fun handleConnection(client: Socket) {
        try {
            client.soTimeout = 15000
            val `in` = client.getInputStream()
            val out = client.getOutputStream()

            val requestLine = readLine(`in`) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val rawPath = parts[1]

            val queryIdx = rawPath.indexOf('?')
            val path = if (queryIdx >= 0) rawPath.substring(0, queryIdx) else rawPath
            val query = if (queryIdx >= 0) rawPath.substring(queryIdx + 1) else ""

            // Headers + body
            val headers = HashMap<String, String>()
            var contentLength = 0
            while (true) {
                val line = readLine(`in`) ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) {
                    val k = line.substring(0, idx).trim().lowercase()
                    val v = line.substring(idx + 1).trim()
                    headers[k] = v
                    if (k == "content-length") contentLength = v.toIntOrNull() ?: 0
                }
            }
            val body = if (contentLength > 0) readBody(`in`, contentLength) else ""

            if (method == "OPTIONS") {
                writeResponse(out, 204, "", hasBody = false)
                return
            }

            val (code, json) = route(method, path, body, query)
            writeResponse(out, code, json)
        } catch (e: Exception) {
        } finally {
            try { client.close() } catch (e: Exception) {}
        }
    }

    private fun readLine(`in`: java.io.InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = `in`.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(b.toChar())
        }
    }

    private fun readBody(`in`: java.io.InputStream, length: Int): String {
        val out = ByteArrayOutputStream()
        var remaining = length
        val buf = ByteArray(1024)
        while (remaining > 0) {
            val n = `in`.read(buf, 0, minOf(remaining, buf.size))
            if (n == -1) break
            out.write(buf, 0, n)
            remaining -= n
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    private fun route(method: String, path: String, body: String, query: String): Pair<Int, String> {
        return when {
            path.startsWith("/api/acc/") -> handleAcc(method, path.removePrefix("/api/acc/"), body, query)
            path == "/api/goai" -> handleGoAi(body)
            path == "/api/update" -> handleUpdate()
            path == "/api/info" -> handleInfo()
            else -> 404 to """{"ok":false,"error":"unknown"}"""
        }
    }

    private fun handleUpdate(): Pair<Int, String> {
        val u = JSONObject()
            .put("ok", true)
            .put("current", "1.1.0")
            .put("channel", "stable")
            .put("checkUrl", "https://raw.githubusercontent.com/GoStudios-Real/GoConsoleOS/main/update.json")
            .put("manifestVersion", 1)
            .put("serverTime", System.currentTimeMillis())
        return 200 to u.toString()
    }

    private fun handleInfo(): Pair<Int, String> {
        val info = JSONObject()
            .put("id", "GCS")
            .put("kind", "console.os")
            .put("name", "GoConsoleOS Portable")
            .put("os", "GoConsoleOS Android")
            .put("version", "1.1.0")
            .put("server", "go-console-acc")
            .put("features", listOf("acc", "goai", "link", "usb", "cast"))
        return 200 to info.toString()
    }

    private fun handleGoAi(body: String): Pair<Int, String> {
        val msg = try { JSONObject(body).optString("message") } catch (e: Exception) { "" }
        val reply = goAi?.reply(msg) ?: "GoAI is unavailable."
        return 200 to JSONObject().put("reply", reply).toString()
    }

    private fun handleAcc(method: String, sub: String, body: String, query: String): Pair<Int, String> {
        val store = store ?: return 500 to """{"ok":false}"""
        val endpoint = sub.split("/")[0]
        var token = try { JSONObject(body).optString("token") } catch (e: Exception) { "" }
        if (token.isEmpty()) token = queryValue(query, "token") ?: ""

        return when {
            endpoint == "register" && method == "POST" -> {
                val (ok, err, user) = store.register(body)
                if (ok && user != null) 200 to JSONObject()
                    .put("ok", true)
                    .put("token", store.createSession(user.optString("username"), "android", ""))
                    .put("profile", user)
                    .toString()
                else 400 to JSONObject().put("ok", false).put("error", err).toString()
            }
            endpoint == "login" && method == "POST" -> {
                val (ok, err, user) = store.login(body)
                if (ok && user != null) 200 to JSONObject()
                    .put("ok", true)
                    .put("token", store.createSession(user.optString("username"), "android", ""))
                    .put("profile", user)
                    .toString()
                else 401 to JSONObject().put("ok", false).put("error", err).toString()
            }
            endpoint == "logout" && method == "POST" -> {
                store.destroySession(token)
                200 to """{"ok":true}"""
            }
            endpoint == "profile" -> {
                val user = store.validate(token)
                when {
                    user == null -> 401 to """{"ok":false,"error":"not authenticated"}"""
                    method == "PATCH" -> {
                        store.updateProfile(user, body)
                        200 to JSONObject().put("ok", true).put("profile", user).toString()
                    }
                    else -> 200 to JSONObject().put("ok", true).put("profile", user).toString()
                }
            }
            endpoint == "wallet" -> {
                val user = store.validate(token)
                if (user == null) 401 to """{"ok":false}"""
                else 200 to JSONObject().put("ok", true).put("points", user.optLong("goPoints")).toString()
            }
            else -> 404 to """{"ok":false,"error":"unknown acc endpoint"}"""
        }
    }

    private fun queryValue(query: String, key: String): String? {
        for (pair in query.split("&")) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val k = if (eq >= 0) pair.substring(0, eq) else pair
            if (k == key) return if (eq >= 0) pair.substring(eq + 1) else ""
        }
        return null
    }

    private fun writeResponse(out: java.io.OutputStream, code: Int, json: String, hasBody: Boolean = true) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val statusLine = when (code) {
            200 -> "HTTP/1.1 200 OK"
            204 -> "HTTP/1.1 204 No Content"
            400 -> "HTTP/1.1 400 Bad Request"
            401 -> "HTTP/1.1 401 Unauthorized"
            404 -> "HTTP/1.1 404 Not Found"
            else -> "HTTP/1.1 500 Internal Server Error"
        }
        val header = "$statusLine\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            (if (hasBody) "Content-Length: ${bytes.size}\r\n" else "Content-Length: 0\r\n") +
            "Connection: close\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Headers: Content-Type\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS\r\n" +
            "\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        if (hasBody) out.write(bytes)
        out.flush()
    }
}
