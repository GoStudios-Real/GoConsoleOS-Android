package com.gostudios.console.cloud

import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    gameTitle: String,
    streamUrl: String,
    onBack: () -> Unit,
) {
    var showOverlay by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (streamUrl.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = error?.description?.toString() ?: "Connection failed"
                                }
                            }
                        }
                        loadUrl(streamUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isLoading && streamUrl.isNotEmpty() && loadError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to stream...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (streamUrl.isEmpty() || loadError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Game Streaming",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Streaming for \"$gameTitle\" will be available soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (loadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ControllerOverlay(
                visible = showOverlay,
                onDismiss = { showOverlay = false },
                onButton = { _, _ -> },
                onAxis = { _, _ -> },
            )
        }

        AnimatedVisibility(
            visible = !showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row {
                    FilledTonalIconButton(
                        onClick = { showOverlay = !showOverlay },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "Toggle Controller",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Streaming",
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        }
    }
}
