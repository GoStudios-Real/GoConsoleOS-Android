package com.gostudios.console.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.Discovery

/**
 * GoConsoleOS Watch Companion - discovers and connects to your GoConsoleOS console.
 */
class WatchActivity : ComponentActivity() {

    private var scanner: Discovery.Scanner? = null
    private val hosts = LinkedHashMap<String, ConsoleHost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val discoveredHosts by remember { mutableStateOf(hosts) }
            val selectedHost = remember { mutableStateOf<ConsoleHost?>(null) }

            LaunchedEffect(Unit) {
                scanner = Discovery.Scanner { host ->
                    hosts[host.id] = host
                    selectedHost.value = host
                }
                scanner?.start()
            }

            GoConsoleWatchTheme {
                if (selectedHost.value != null) {
                    ConsoleStatusScreen(
                        host = selectedHost.value!!,
                        onDismiss = { selectedHost.value = null }
                    )
                } else {
                    DiscoveryScreen(
                        hosts = discoveredHosts,
                        onHostSelected = { selectedHost.value = it }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        scanner?.stop()
        super.onDestroy()
    }
}

@Composable
fun GoConsoleWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF00C9DB),
            onPrimary = Color(0xFF0D0D14),
            surface = Color(0xFF1A1A2E),
            onSurface = Color(0xFFF0F0FF),
            background = Color(0xFF0D0D14),
            onBackground = Color(0xFFF0F0FF)
        )
    ) {
        content()
    }
}