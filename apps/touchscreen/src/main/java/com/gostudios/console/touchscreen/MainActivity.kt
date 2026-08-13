package com.gostudios.console.touchscreen

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.Discovery
import com.gostudios.console.sdk.LinkClient
import com.gostudios.console.sdk.Protocol
import org.json.JSONObject

/**
 * GoConsoleOS USB Controller: discovers a GoConsoleOS host on the LAN, opens a
 * Link TCP stream and pumps touch-gamepad state (buttons + analog sticks) to it
 * as FRAME_INPUT.
 */
class MainActivity : AppCompatActivity() {

    private var scanner: Discovery.Scanner? = null
    private var client: LinkClient? = null
    private var activeHost: ConsoleHost? = null

    private lateinit var txtStatus: TextView
    private lateinit var pad: GamePadView
    private lateinit var btnReconnect: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        btnReconnect = findViewById(R.id.btnReconnect)
        pad = findViewById(R.id.pad)

        pad.onStateChanged = { frame ->
            client?.sendFrame(Protocol.FRAME_INPUT, frame)
        }

        btnReconnect.setOnClickListener { connect() }
        connect()
    }

    private fun connect() {
        stopLink()
        activeHost = null
        txtStatus.text = getString(R.string.status_scanning)

        scanner = Discovery.Scanner { host ->
            runOnUiThread {
                if (activeHost == null) {
                    activeHost = host
                    txtStatus.text = getString(R.string.status_connecting, host.name)
                    startLink(host)
                }
            }
        }
        scanner?.start()
    }

    private fun startLink(host: ConsoleHost) {
        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                runOnUiThread {
                    txtStatus.text = getString(R.string.status_connected, h.name)
                }
            }

            override fun onControl(type: String, payload: JSONObject) = Unit

            override fun onFrame(type: Int, payload: ByteArray) = Unit

            override fun onDisconnected(error: String?) {
                runOnUiThread {
                    txtStatus.text = getString(R.string.status_disconnected)
                }
            }
        })
        client?.connect()
    }

    private fun stopLink() {
        scanner?.stop()
        scanner = null
        client?.disconnect()
        client = null
    }

    override fun onDestroy() {
        stopLink()
        super.onDestroy()
    }
}
