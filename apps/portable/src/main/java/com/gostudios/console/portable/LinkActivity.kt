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
import org.json.JSONObject

/**
 * GoConsoleOS Link — connects to the remote host, pulls the game catalogue and
 * can launch a title by sending a control message back to the host.
 */
class LinkActivity : AppCompatActivity() {

    private var client: LinkClient? = null
    private lateinit var adapter: TitleAdapter
    private val titles = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link)

        val hostJson = intent.getStringExtra(EXTRA_HOST) ?: return
        val host = ConsoleHost.fromJson(JSONObject(hostJson))

        val rv = findViewById<RecyclerView>(R.id.gamesList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TitleAdapter(titles) { title ->
            client?.sendControl("games.launch", JSONObject().put("title", title))
        }
        rv.adapter = adapter

        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                runOnUiThread {
                    findViewById<TextView>(R.id.txtConn).text = "Connected to ${h.name}"
                }
                client?.sendControl("games.list")
            }
            override fun onControl(type: String, payload: JSONObject) {
                if (type == "games.list") {
                    val arr = payload.optJSONArray("games")
                    val fresh = mutableListOf<String>()
                    if (arr != null) for (i in 0 until arr.length()) fresh.add(arr.getString(i))
                    runOnUiThread {
                        titles.clear()
                        titles.addAll(fresh)
                        adapter.notifyDataSetChanged()
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

    companion object {
        private const val EXTRA_HOST = "hostJson"

        fun intent(context: Context, host: ConsoleHost): Intent =
            Intent(context, LinkActivity::class.java)
                .putExtra(EXTRA_HOST, host.toJson().toString())
    }

    private class TitleAdapter(
        private val items: MutableList<String>,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<TitleAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById<TextView>(R.id.gameTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_game, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val title = items[position]
            holder.title.text = title
            holder.itemView.setOnClickListener { onClick(title) }
        }

        override fun getItemCount() = items.size
    }
}