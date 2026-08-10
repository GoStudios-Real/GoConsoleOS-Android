package com.gostudios.console.portable

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.portable.cast.MediaProjectionActivity
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.DeviceServer
import com.gostudios.console.sdk.Discovery

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
            activeHost?.let { startActivity(PortableActivity.intent(this, it)) }
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

    override fun onDestroy() {
        DeviceServer.stop()
        scanner?.stop()
        super.onDestroy()
    }
}