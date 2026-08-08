package com.gostudios.console.portable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.LinkClient
import com.gostudios.console.sdk.UsbDeviceInfo
import com.gostudios.console.sdk.UsbHealthLocal
import java.util.Locale
import org.json.JSONObject

/**
 * USB Health — combines the local (on-device) volume enumeration with the
 * host's deep SMART report for portable USB drives attached to the PC.
 */
class UsbHealthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        showLocal()

        val hostJson = intent.getStringExtra(EXTRA_HOST)
        if (hostJson != null) {
            showRemote(ConsoleHost.fromJson(JSONObject(hostJson)))
        }
    }

    private fun showLocal() {
        val devices = UsbHealthLocal.enumerate(this)
        val sb = StringBuilder("On this device (${devices.size}):\n")
        for (d in devices) {
            sb.append("  • ").append(d.label)
                .append(" [").append(d.interfaceType).append("]")
                .append(" health=").append(d.health)
            if (d.totalBytes > 0) {
                sb.append(" ").append(formatBytes(d.freeBytes))
                    .append(" free / ").append(formatBytes(d.totalBytes))
            }
            sb.append("\n")
        }
        findViewById<TextView>(R.id.txtLocal).text = sb.toString()
    }

    private fun showRemote(host: ConsoleHost) {
        var client: LinkClient? = null
        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                client?.sendControl("usb.list")
            }
            override fun onControl(type: String, payload: JSONObject) {
                if (type == "usb.list") {
                    val arr = payload.optJSONArray("devices")
                    val devices = UsbDeviceInfo.fromArray(arr ?: org.json.JSONArray())
                    runOnUiThread {
                        val sb = StringBuilder("On host (${host.name}):\n")
                        if (devices.isEmpty()) sb.append("  (no USB drives reported)\n")
                        for (d in devices) {
                            sb.append("  • ").append(d.label)
                                .append(" health=").append(d.health)
                                .append(" score=").append(d.healthScore)
                                .append(" ").append(d.issue)
                                .append("\n")
                        }
                        findViewById<TextView>(R.id.txtRemote).text = sb.toString()
                    }
                }
            }
            override fun onFrame(type: Int, payload: ByteArray) {}
            override fun onDisconnected(error: String?) {
                runOnUiThread {
                    findViewById<TextView>(R.id.txtRemote).text = "Host unreachable: $error"
                }
            }
        })
        client.connect()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var u = 0
        while (v >= 1024 && u < units.size - 1) { v /= 1024; u++ }
        return String.format(Locale.US, "%.1f %s", v, units[u])
    }

    companion object {
        private const val EXTRA_HOST = "hostJson"

        fun intent(context: Context, host: ConsoleHost): Intent =
            Intent(context, UsbHealthActivity::class.java)
                .putExtra(EXTRA_HOST, host.toJson().toString())
    }
}