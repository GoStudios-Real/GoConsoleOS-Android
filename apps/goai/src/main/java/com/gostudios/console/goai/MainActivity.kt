package com.gostudios.console.goai

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * GoAI - the AI for GoConsoleOS, by GoStudios.
 *
 * Loads the GoAI web app (the same code the desktop/Electron build ships)
 * inside a hardened WebView. No Electron APIs are used by the web app, so
 * everything runs unmodified on Android.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)

        // Ask for the permissions the web app uses (voice chat + location) up front.
        requestRuntimePermissions()

        configureWebView()

        // Back button: navigate the web view first, otherwise background the app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    moveTaskToBack(true)
                }
            }
        })

        loadMainPage()
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
        }
    }

    private fun configureWebView() {
        val ws: WebSettings = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.loadWithOverviewMode = true
        ws.useWideViewPort = true
        ws.mediaPlaybackRequiresUserGesture = false
        try {
            ws.javaClass.getMethod("setAppCacheEnabled", java.lang.Boolean.TYPE).invoke(ws, java.lang.Boolean.TRUE)
        } catch (e: Exception) {
            // not available on newer SDKs
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { ws.offscreenPreRaster = false } catch (e: Exception) { }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { ws.safeBrowsingEnabled = true } catch (e: Exception) { }
        }

        // Memory-aware rendering: software on low-memory devices, hardware otherwise.
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (am.memoryClass < 128) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        } else {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        webView.setBackgroundColor(Color.WHITE)

        webView.webViewClient = object : WebViewClient() {
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                // Renderer crashed (often low memory). Restart the activity instead of dying.
                runOnUiThread {
                    try {
                        recreate()
                    } catch (e: Exception) {
                        finish()
                    }
                }
                return true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    loadFallback()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                // Grant microphone/camera so voice chat works.
                runOnUiThread { request.grant(request.resources) }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                runOnUiThread { callback.invoke(origin, true, false) }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                return true
            }

            override fun onJsConfirm(view: WebView, url: String, message: String, result: android.webkit.JsResult): Boolean {
                result.confirm()
                return true
            }
        }
    }

    private fun loadMainPage() {
        try {
            webView.loadUrl("file:///android_asset/www/index.html")
        } catch (e: Exception) {
            loadFallback()
        }
    }

    private fun loadFallback() {
        try {
            webView.loadUrl("file:///android_asset/www/assets/fallback.html")
        } catch (e: Exception) {
            Toast.makeText(this, "App failed to load", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}