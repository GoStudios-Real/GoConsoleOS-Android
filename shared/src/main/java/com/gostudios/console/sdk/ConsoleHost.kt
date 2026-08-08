package com.gostudios.console.sdk

import org.json.JSONObject

/**
 * Description of a reachable GoConsoleOS host on the local network,
 * announced via UDP and returned from the discovery binder.
 */
data class ConsoleHost(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = Protocol.LINK_PORT,
    val version: String = "",
    val features: List<String> = emptyList(),
    val osLabel: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("address", address)
        .put("port", port)
        .put("version", version)
        .put("features", JSONObject().put("arr", features).getJSONArray("arr"))
        .put("os", osLabel)

    companion object {
        fun fromJson(obj: JSONObject): ConsoleHost {
            val arr = obj.optJSONArray("features")
            val feats = mutableListOf<String>()
            if (arr != null) for (i in 0 until arr.length()) feats.add(arr.getString(i))
            return ConsoleHost(
                id = obj.optString("id", obj.optString("address", "unknown")),
                name = obj.optString("name", "GoConsoleOS"),
                address = obj.optString("address", ""),
                port = obj.optInt("port", Protocol.LINK_PORT),
                version = obj.optString("version", ""),
                features = feats,
                osLabel = obj.optString("os", ""),
            )
        }
    }
}