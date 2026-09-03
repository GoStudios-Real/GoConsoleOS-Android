package com.gostudios.console.cloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gostudios.console.sdk.ConsoleHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoConsoleTheme {
                CloudGamingApp()
            }
        }
    }
}

enum class Screen {
    DISCOVERY,
    LIBRARY,
    STREAM,
    DASHBOARD,
}

@Composable
fun CloudGamingApp() {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    DisposableEffect(Unit) { onDispose { scope.cancel() } }

    var currentScreen by remember { mutableStateOf(Screen.DISCOVERY) }
    var selectedConsole by remember { mutableStateOf<ConsoleHost?>(null) }
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }

    val connection = remember { ConsoleConnection() }
    var foundConsoles by remember { mutableStateOf<List<ConsoleHost>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var games by remember { mutableStateOf<List<GameInfo>>(emptyList()) }
    var isLoadingGames by remember { mutableStateOf(false) }
    var gameError by remember { mutableStateOf<String?>(null) }
    var isConnectingCloud by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            connection.stopDiscovery()
        }
    }

    fun loadGames() {
        isLoadingGames = true
        gameError = null
        scope.launch {
            val result = connection.fetchGames()
            games = result
            isLoadingGames = false
            if (result.isEmpty()) {
                gameError = "No games found or connection failed."
            }
        }
    }

    fun startScanning() {
        isScanning = true
        foundConsoles = emptyList()
        connection.startDiscovery { host ->
            foundConsoles = foundConsoles.filter { it.id != host.id } + host
        }
    }

    fun stopScanning() {
        isScanning = false
        connection.stopDiscovery()
    }

    fun connectToConsole(host: ConsoleHost) {
        stopScanning()
        selectedConsole = host
        connection.connect(host)
        currentScreen = Screen.LIBRARY
        loadGames()
    }

    fun connectToCloud() {
        isConnectingCloud = true
        cloudError = null
        scope.launch {
            val success = connection.connectToCloud()
            isConnectingCloud = false
            if (success) {
                selectedConsole = null
                currentScreen = Screen.DASHBOARD
            } else {
                cloudError = "Could not connect to cloud server."
            }
        }
    }

    fun launchGame(game: GameInfo) {
        selectedGame = game
        currentScreen = Screen.STREAM
    }

    fun navigateBack() {
        when (currentScreen) {
            Screen.STREAM -> {
                currentScreen = Screen.LIBRARY
                selectedGame = null
            }
            Screen.LIBRARY -> {
                currentScreen = Screen.DISCOVERY
                selectedConsole = null
                games = emptyList()
                connection.disconnect()
            }
            Screen.DASHBOARD -> {
                currentScreen = Screen.DISCOVERY
                connection.disconnect()
            }
            Screen.DISCOVERY -> {}
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() + slideInHorizontally { if (targetState == Screen.DISCOVERY) -it else it } togetherWith
                fadeOut() + slideOutHorizontally { if (targetState == Screen.DISCOVERY) it else -it }
        },
        modifier = Modifier.fillMaxSize(),
        label = "screen_transition",
    ) { screen ->
        when (screen) {
            Screen.DISCOVERY -> DiscoveryScreen(
                foundConsoles = foundConsoles,
                isScanning = isScanning,
                onScanToggle = {
                    if (isScanning) stopScanning() else startScanning()
                },
                onConnect = ::connectToConsole,
                onConnectCloud = ::connectToCloud,
                isConnectingCloud = isConnectingCloud,
                cloudError = cloudError,
            )

            Screen.LIBRARY -> GameLibraryScreen(
                consoleName = selectedConsole?.name ?: "Console",
                games = games,
                isLoading = isLoadingGames,
                error = gameError,
                onBack = ::navigateBack,
                onGameLaunch = ::launchGame,
                onRefresh = ::loadGames,
            )

            Screen.STREAM -> StreamScreen(
                gameTitle = selectedGame?.title ?: "Game",
                streamUrl = connection.getStreamUrl("/stream"),
                onBack = ::navigateBack,
            )

            Screen.DASHBOARD -> DashboardScreen(
                dashboardUrl = connection.getDashboardUrl(),
                consoleName = connection.cloudServer?.name ?: "Console",
                onBack = ::navigateBack,
                onLaunchGame = ::launchGame,
            )
        }
    }
}