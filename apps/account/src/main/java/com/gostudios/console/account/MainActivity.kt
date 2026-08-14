package com.gostudios.console.account

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.Discovery
import java.util.concurrent.Executors

/**
 * GoConsoleOS Account Center launcher. Discovers a GoConsoleOS host on the LAN
 * (or accepts a manual IP) and opens the Account Center web portal in a WebView.
 */
class MainActivity : AppCompatActivity() {

    private var scanner: Discovery.Scanner? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtIp = findViewById<EditText>(R.id.edtIp)
        val edtPort = findViewById<EditText>(R.id.edtPort)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val txtScan = findViewById<TextView>(R.id.txtScan)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            val host = edtIp.text?.toString()?.trim().orEmpty()
            if (host.isEmpty()) {
                txtStatus.text = getString(R.string.connect_fail, host)
                return@setOnClickListener
            }
            val port = edtPort.text?.toString()?.trim()?.toIntOrNull() ?: 39210
            connect(host, port, txtStatus)
        }

        // Auto-discover hosts; prefill the IP when one is found.
        scanner = Discovery.Scanner { host ->
            runOnUiThread {
                txtScan.text = getString(R.string.connected, host.address)
                edtIp.setText(host.address)
            }
        }
        scanner?.start()
    }

    private fun connect(host: String, port: Int, status: TextView) {
        val normalized = if (host.startsWith("http://") || host.startsWith("https://")) host
        else "http://$host"
        val url = "$normalized:$port/"
        status.text = getString(R.string.connected, host)
        executor.execute {
            try {
                val conn = java.net.URL(url).openConnection()
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = (conn as java.net.HttpURLConnection).responseCode
                conn.disconnect()
                if (code < 200 || code >= 400) {
                    runOnUiThread { status.text = getString(R.string.connect_fail, "$host ($code)") }
                    return@execute
                }
                runOnUiThread {
                    startActivity(Intent(this, PortalActivity::class.java)
                        .putExtra("url", url))
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = getString(R.string.connect_fail, "$host - ${e.message}") }
            }
        }
    }

    override fun onDestroy() {
        scanner?.stop()
        executor.shutdown()
        super.onDestroy()
    }
}