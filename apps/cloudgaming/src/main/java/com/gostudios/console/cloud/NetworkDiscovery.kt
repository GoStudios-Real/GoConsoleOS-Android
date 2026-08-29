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

class ConsoleConnection {
    var host: ConsoleHost? = null
        private set
    var isConnected: Boolean = false
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var scanner: Discovery.Scanner? = null

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
    }

    fun disconnect() {
        host = null
        isConnected = false
    }

    suspend fun fetchGames(): List<GameInfo> = withContext(Dispatchers.IO) {
        val currentHost = host ?: return@withContext emptyList()
        try {
            val url = "http://${currentHost.address}:${currentHost.port}/api/games"
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
        val currentHost = host ?: return@withContext false
        try {
            val url = "http://${currentHost.address}:${currentHost.port}/api/games/launch"
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
        val currentHost = host ?: return ""
        return "http://${currentHost.address}:${currentHost.port}$path"
    }
}
