package com.gostudios.console.sdk

import org.json.JSONArray
import org.json.JSONObject

/**
 * A USB device or mounted volume reported by the host (GoConsoleOS USB Health)
 * or enumerated locally on the phone/TV via UsbManager.
 */
data class UsbDeviceInfo(
    val id: String,
    val label: String,
    val vendor: String = "",
    val product: String = "",
    val serial: String = "",
    val health: String = Protocol.USB_HEALTH_UNKNOWN,
    val healthScore: Int = 0,
    val totalBytes: Long = 0,
    val freeBytes: Long = 0,
    val interfaceType: String = "",
    val issue: String = "",
    val mounted: Boolean = true,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)

    fun toJson() = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("vendor", vendor)
        .put("product", product)
        .put("serial", serial)
        .put("health", health)
        .put("healthScore", healthScore)
        .put("total", totalBytes)
        .put("free", freeBytes)
        .put("interface", interfaceType)
        .put("issue", issue)
        .put("mounted", mounted)

    companion object {
        fun fromArray(arr: JSONArray): List<UsbDeviceInfo> {
            val out = mutableListOf<UsbDeviceInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    UsbDeviceInfo(
                        id = o.optString("id"),
                        label = o.optString("label"),
                        vendor = o.optString("vendor"),
                        product = o.optString("product"),
                        serial = o.optString("serial"),
                        health = o.optString("health", Protocol.USB_HEALTH_UNKNOWN),
                        healthScore = o.optInt("healthScore", 0),
                        totalBytes = o.optLong("total", 0),
                        freeBytes = o.optLong("free", 0),
                        interfaceType = o.optString("interface"),
                        issue = o.optString("issue"),
                        mounted = o.optBoolean("mounted", true),
                    )
                )
            }
            return out
        }
    }
}