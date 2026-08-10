package com.gostudios.console.portable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.LinkClient
import com.gostudios.console.sdk.Protocol
import org.json.JSONArray
import org.json.JSONObject

/**
 * GoConsoleOS Portable — full tool panel for the remote host.
 *
 * Connects to the GoConsoleOS console on the LAN, lists the host tools it can
 * drive (GoUsbMaker / USB Health / Cast / GoAI / Store / Screenshot / ...) and
 * triggers the selected one by sending a control message back to the host.
 */
class PortableActivity : AppCompatActivity() {

    private var client: LinkClient? = null
    private lateinit var adapter: ToolAdapter
    private val tools = mutableListOf<ToolRow>()

    /** Default tool catalogue shown when the host does not support `tools.list`. */
    private val defaults = listOf(
        ToolRow("usb-installer", "GoUsbMaker", "Build a Portable USB Gaming Console"),
        ToolRow("usb-health", "USB Health", "SMART report for every USB console"),
        ToolRow("cast", "GoConsoleOS Cast", "Mirror your console to a TV or device"),
        ToolRow("goai", "GoAI", "Ask your assistant anything, locally"),
        ToolRow("store", "GoStore", "Browse the curated app catalogue"),
        ToolRow("screenshot", "Screenshot", "Capture the current screen on the host"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portable)

        val hostJson = intent.getStringExtra(EXTRA_HOST) ?: return
        val host = ConsoleHost.fromJson(JSONObject(hostJson))

        val rv = findViewById<RecyclerView>(R.id.toolList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ToolAdapter(tools) { tool -> runTool(tool) }
        rv.adapter = adapter

        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                runOnUiThread {
                    findViewById<TextView>(R.id.txtConn).text =
                        getString(R.string.status_connected, h.name)
                }
                client?.sendControl(MSG_TOOLS_LIST)
            }

            override fun onControl(type: String, payload: JSONObject) {
                if (type == MSG_TOOLS_LIST) {
                    applyTools(payload.optJSONArray("tools"))
                } else if (type == MSG_TOOLS_RUN) {
                    runOnUiThread {
                        findViewById<TextView>(R.id.txtConn).text =
                            getString(R.string.tool_ok, payload.optString("tool", ""))
                    }
                }
            }

            override fun onFrame(type: Int, payload: ByteArray) {}
            override fun onDisconnected(error: String?) {
                runOnUiThread {
                    findViewById<TextView>(R.id.txtConn).text = "Disconnected: $error"
                }
            }
        })
        client?.connect()
    }

    override fun onDestroy() {
        client?.close()
        super.onDestroy()
    }

    private fun runTool(tool: ToolRow) {
        val c = client ?: return
        if (tool.id == "usb-installer") {
            c.sendControl(Protocol.MSG_PAIR, JSONObject().put("action", "open-usb-installer"))
        } else {
            c.sendControl(MSG_TOOLS_RUN, JSONObject().put("tool", tool.id))
        }
        findViewById<TextView>(R.id.txtConn).text =
            getString(R.string.tool_running, tool.name)
    }

    private fun applyTools(arr: JSONArray?) {
        val rows: List<ToolRow> = if (arr == null || arr.length() == 0) {
            defaults
        } else {
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    add(
                        ToolRow(
                            item.optString("id", ""),
                            item.optString("name", ""),
                            item.optString("desc", ""),
                        ),
                    )
                }
            }.ifEmpty { defaults }
        }
        runOnUiThread {
            tools.clear()
            tools.addAll(rows)
            adapter.notifyDataSetChanged()
        }
    }

    private class ToolRow(val id: String, val name: String, val desc: String)

    private class ToolAdapter(
        private val items: MutableList<ToolRow>,
        private val onClick: (ToolRow) -> Unit,
    ) : RecyclerView.Adapter<ToolAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById<TextView>(R.id.toolName)
            val desc: TextView = view.findViewById<TextView>(R.id.toolDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tool, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val tool = items[position]
            holder.name.text = tool.name
            holder.desc.text = tool.desc
            holder.itemView.setOnClickListener { onClick(tool) }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val EXTRA_HOST = "hostJson"
        private const val MSG_TOOLS_LIST = "tools.list"
        private const val MSG_TOOLS_RUN = "tools.run"

        fun intent(context: Context, host: ConsoleHost): Intent =
            Intent(context, PortableActivity::class.java)
                .putExtra(EXTRA_HOST, host.toJson().toString())
    }
}