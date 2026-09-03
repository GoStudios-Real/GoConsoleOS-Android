package com.gostudios.console.cloud

import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.Discovery
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GameInfo(
    val title: String,
    val platform: String = "",
    val coverUrl: String = "",
    val launchUrl: String = "",
)

data class CloudServer(
    val address: String,
    val port: Int,
    val name: String = "Cloud",
    val version: String = "2.2.0",
)

enum class ConnectionMode {
    LOCAL,
    CLOUD,
}

class ConsoleConnection {
    var host: ConsoleHost? = null
        private set
    var isConnected: Boolean = false
        private set
    var connectionMode: ConnectionMode = ConnectionMode.LOCAL
        private set
    var cloudServer: CloudServer? = null
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var scanner: Discovery.Scanner? = null

    private val CLOUD_SERVER_URL = "https://gostudios.net/api"

    fun startDiscovery(onHostFound: (ConsoleHost) -> Unit) {
        stopDiscovery()
        scanner = Discovery.Scanner { host ->
            onHostFound(host)
        }
        scanner?.start()
    }

    fun stopDiscovery() {
        scanner?.close()
        scanner = null
    }

    fun connect(host: ConsoleHost) {
        this.host = host
        this.isConnected = true
        this.connectionMode = ConnectionMode.LOCAL
        this.cloudServer = null
    }

    suspend fun connectToCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$CLOUD_SERVER_URL/console")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext false
            val json = JSONObject(body)
            val success = json.optBoolean("success", false)
            if (success) {
                this@ConsoleConnection.cloudServer = CloudServer(
                    address = CLOUD_SERVER_URL,
                    port = 443,
                    name = json.optString("name", "GoConsoleOS Cloud"),
                    version = json.optString("version", "2.2.0"),
                )
                this@ConsoleConnection.isConnected = true
                this@ConsoleConnection.connectionMode = ConnectionMode.CLOUD
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        host = null
        cloudServer = null
        isConnected = false
        connectionMode = ConnectionMode.LOCAL
    }

    fun getDashboardUrl(): String {
        return when (connectionMode) {
            ConnectionMode.LOCAL -> {
                val currentHost = host ?: return ""
                "http://${currentHost.address}:${currentHost.port}"
            }
            ConnectionMode.CLOUD -> CLOUD_SERVER_URL
        }
    }

    suspend fun fetchGames(): List<GameInfo> = withContext(Dispatchers.IO) {
        try {
            val url = when (connectionMode) {
                ConnectionMode.LOCAL -> {
                    val currentHost = host ?: return@withContext emptyList()
                    "http://${currentHost.address}:${currentHost.port}/api/games"
                }
                ConnectionMode.CLOUD -> "$CLOUD_SERVER_URL/games"
            }
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val gamesArray = json.optJSONArray("games") ?: return@withContext emptyList()
            val games = mutableListOf<GameInfo>()
            for (i in 0 until gamesArray.length()) {
                val obj = gamesArray.optJSONObject(i)
                if (obj != null) {
                    games.add(
                        GameInfo(
                            title = obj.optString("title", "Unknown"),
                            platform = obj.optString("platform", ""),
                            coverUrl = obj.optString("cover", ""),
                            launchUrl = obj.optString("url", ""),
                        )
                    )
                } else {
                    val name = gamesArray.optString(i)
                    if (name.isNotEmpty()) {
                        games.add(GameInfo(title = name))
                    }
                }
            }
            games
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun launchGame(title: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = when (connectionMode) {
                ConnectionMode.LOCAL -> {
                    val currentHost = host ?: return@withContext false
                    "http://${currentHost.address}:${currentHost.port}/api/games/launch"
                }
                ConnectionMode.CLOUD -> "$CLOUD_SERVER_URL/games/launch"
            }
            val json = JSONObject().put("title", title)
            val body = json.toString().toRequestBody(
                "application/json; charset=utf-8".toMediaTypeOrNull()
            )
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun getStreamUrl(path: String = ""): String {
        return when (connectionMode) {
            ConnectionMode.LOCAL -> {
                val currentHost = host ?: return ""
                "http://${currentHost.address}:${currentHost.port}$path"
            }
            ConnectionMode.CLOUD -> "$CLOUD_SERVER_URL$path"
        }
    }
}