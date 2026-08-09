package com.gostudios.console.sdk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistent ACC (account) store for GoConsoleOS Portable. Users, sessions,
 * and the wallet live in app-private SharedPreferences as JSON, so the phone
 * or TV box itself can hold accounts without any cloud dependency.
 *
 * Mirrors the desktop {GoConsoleOS.Shared.Acc.AccStore}.
 */
class AccStore(context: Context) {

    private val prefs = context.getSharedPreferences("go_console_acc", Context.MODE_PRIVATE)

    private fun loadUsers(): JSONObject {
        val raw = prefs.getString("users", "{}")
        return try { JSONObject(raw.orEmpty()) } catch (e: Exception) { JSONObject() }
    }

    private fun saveUsers(users: JSONObject) {
        prefs.edit().putString("users", users.toString()).apply()
    }

    /** Register a user from a JSON request body. Returns (ok, error, userJson or null). */
    fun register(body: String): Triple<Boolean, String, JSONObject?> {
        val req = try { JSONObject(body) } catch (e: Exception) { return Triple(false, "invalid json", null) }
        val username = req.optString("username").trim()
        val email = req.optString("email").trim()
        val password = req.optString("password")
        val displayName = req.optString("displayName").ifBlank { username }

        if (username.isEmpty() || password.length < 6) return Triple(false, "username required and password must be 6+ chars", null)

        val users = loadUsers()
        val existing = users.keys().asSequence().map { users.get(it) as JSONObject }
            .any { it.optString("username").equals(username, true) || it.optString("email").equals(email, true) }
        if (existing) return Triple(false, "username or email already registered", null)

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val user = JSONObject()
            .put("id", id)
            .put("username", username)
            .put("email", email)
            .put("passwordHash", password.hashCode().toString())
            .put("displayName", displayName)
            .put("createdUtc", now)
            .put("lastLoginUtc", now)
            .put("goPoints", 1000)
            .put("tier", "casual")
        users.put(id, user)
        saveUsers(users)
        return Triple(true, "", user)
    }

    /** Log in. Returns (ok, error, userJson or null). */
    fun login(body: String): Triple<Boolean, String, JSONObject?> {
        val req = try { JSONObject(body) } catch (e: Exception) { return Triple(false, "invalid json", null) }
        val username = req.optString("username").trim()
        val password = req.optString("password")
        val users = loadUsers()
        val user = users.keys().asSequence().map { users.get(it) as JSONObject }
            .firstOrNull { it.optString("username").equals(username, true) && it.optString("passwordHash") == password.hashCode().toString() }
            ?: return Triple(false, "invalid credentials", null)
        user.put("lastLoginUtc", System.currentTimeMillis())
        saveUsers(users)
        return Triple(true, "", user)
    }

    /** Create a session token for the given username. */
    fun createSession(username: String, device: String, deviceId: String): String {
        val users = loadUsers()
        val user = users.keys().asSequence().map { users.get(it) as JSONObject }
            .firstOrNull { it.optString("username").equals(username, true) }
            ?: return ""
        val token = "goacc_${UUID.randomUUID()}"
        val sessions = prefs.getString("sessions", "{}")
        val map = try { JSONObject(sessions.orEmpty()) } catch (e: Exception) { JSONObject() }
        map.put(token, JSONObject().put("userId", user.optString("id")).put("device", device).put("deviceId", deviceId))
        prefs.edit().putString("sessions", map.toString()).apply()
        return token
    }

    fun destroySession(token: String) {
        val sessions = prefs.getString("sessions", "{}")
        val map = try { JSONObject(sessions.orEmpty()) } catch (e: Exception) { JSONObject() }
        map.remove(token)
        prefs.edit().putString("sessions", map.toString()).apply()
    }

    /** Validate a token and return the user JSON, or null. */
    fun validate(token: String): JSONObject? {
        if (token.isEmpty()) return null
        val sessions = prefs.getString("sessions", "{}")
        val map = try { JSONObject(sessions.orEmpty()) } catch (e: Exception) { JSONObject() }
        val session = map.optJSONObject(token) ?: return null
        val users = loadUsers()
        return users.optJSONObject(session.optString("userId"))
    }

    fun updateProfile(user: JSONObject, body: String) {
        val req = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
        val users = loadUsers()
        val stored = users.optJSONObject(user.optString("id")) ?: return
        if (req.has("displayName")) stored.put("displayName", req.optString("displayName"))
        if (req.has("email")) stored.put("email", req.optString("email"))
        if (req.has("goPoints")) stored.put("goPoints", req.optLong("goPoints"))
        users.put(stored.optString("id"), stored)
        saveUsers(users)
    }
}

/** Minimal local GoAI assistant for the on-device server. */
class GoAiLocal {

    fun reply(message: String): String {
        val q = message.lowercase()
        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") ->
                "Hello! I'm GoAI, your on-device gaming assistant. Ask me about games, tips, or how to use GoConsoleOS."
            q.contains("game") && (q.contains("recommend") || q.contains("suggest") || q.contains("good")) ->
                "Here are some picks based on your library: racing sims, adventure RPGs, and indie platformers. I can search the store when you connect to a host!"
            q.contains("tip") || q.contains("help") || q.contains("trick") ->
                "Quick tip: keep your console's storage under 80% full for best performance, and check USB health weekly in the Health app."
            q.contains("usb") || q.contains("drive") || q.contains("storage") ->
                "I can report USB and storage health on this device. Open USB Health in the launcher to see drive status and free space."
            q.contains("link") || q.contains("cast") || q.contains("host") ->
                "Use the Link app to pair with a GoConsoleOS host on your network, or Cast to stream to a TV. I live on this device too!"
            q.contains("account") || q.contains("acc") || q.contains("login") ->
                "You can register or sign in with an ACC account right here on this device at http://localhost:39210"
            q.contains("points") || q.contains("wallet") || q.contains("coin") ->
                "Your Go Points wallet is stored on-device with your ACC account. Earn points by playing and completing achievements."
            q.contains("who") && q.contains("you") ->
                "I'm GoAI, the gaming AI assistant built into GoConsoleOS Portable. I run entirely on this device."
            q.contains("version") || q.contains("update") ->
                "GoConsoleOS Portable is version 1.1.0. Check the host for new features like the ACC account system and GoAI."
            else -> "I'm still learning about that. Try asking about games, tips, USB health, linking a host, or your ACC account."
        }
    }
}
