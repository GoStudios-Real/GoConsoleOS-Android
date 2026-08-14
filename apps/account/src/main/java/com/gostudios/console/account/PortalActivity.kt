package com.gostudios.console.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the GoConsoleOS Account Center web portal (served by the console on
 * port 39210) inside a WebView.
 */
class PortalActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portal)

        val url = intent.getStringExtra("url") ?: return

        val web = findViewById<WebView>(R.id.webView)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = WebViewClient()

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (web.canGoBack()) web.goBack() else finish()
        }
        findViewById<Button>(R.id.btnReload).setOnClickListener { web.reload() }

        web.loadUrl(url)
    }

    override fun onBackPressed() {
        val web = findViewById<WebView>(R.id.webView)
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}