package com.gostudios.console.portable

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.portable.cast.MediaProjectionActivity
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.DeviceServer
import com.gostudios.console.sdk.Discovery
import com.gostudios.console.sdk.LinkClient
import com.gostudios.console.sdk.Protocol
import org.json.JSONObject

/**
 * Home screen of GoConsoleOS Portable. Discovers a GoConsoleOS host on the LAN
 * and acts as a launcher for Link / USB Health / Cast / USB Installer.
 */
class MainActivity : AppCompatActivity() {

    private var scanner: Discovery.Scanner? = null
    private var activeHost: ConsoleHost? = null
    private val hosts = LinkedHashMap<String, ConsoleHost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeviceServer.start(applicationContext)
        findViewById<TextView>(R.id.txtServer).text =
            getString(R.string.status_server, DeviceServer.PORT)

        findViewById<LinearLayout>(R.id.btnLink).setOnClickListener {
            activeHost?.let { startActivity(LinkActivity.intent(this, it)) }
        }
        findViewById<LinearLayout>(R.id.btnUsbHealth).setOnClickListener {
            activeHost?.let { startActivity(UsbHealthActivity.intent(this, it)) }
        }
        findViewById<LinearLayout>(R.id.btnCast).setOnClickListener {
            activeHost?.let { startActivity(MediaProjectionActivity.intent(this, it)) }
        }
        findViewById<LinearLayout>(R.id.btnBoot).setOnClickListener {
            activeHost?.let { openUsbInstaller(it) }
        }

        scanner = Discovery.Scanner { host ->
            runOnUiThread {
                hosts[host.id] = host
                activeHost = host
                findViewById<TextView>(R.id.txtStatus).text =
                    getString(R.string.status_connected, host.address)
                findViewById<TextView>(R.id.txtHosts).text =
                    hosts.values.joinToString("\n") { h ->
                        "${h.name} @ ${h.address}:${h.port} (${h.version})"
                    }
            }
        }
        scanner?.start()
    }

    /** Ask the host to open its bundled GoConsoleOS USB Installer tool. */
    private fun openUsbInstaller(host: ConsoleHost) {
        var client: LinkClient? = null
        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                client?.sendControl(Protocol.MSG_PAIR, JSONObject().put("action", "open-usb-installer"))
                client?.close()
            }
            override fun onControl(type: String, payload: JSONObject) {}
            override fun onFrame(type: Int, payload: ByteArray) {}
            override fun onDisconnected(error: String?) {}
        })
        client.connect()
    }

    override fun onDestroy() {
        DeviceServer.stop()
        scanner?.stop()
        super.onDestroy()
    }
}