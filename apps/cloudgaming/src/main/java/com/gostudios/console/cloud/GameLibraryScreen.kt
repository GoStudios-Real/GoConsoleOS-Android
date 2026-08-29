package com.gostudios.console.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    consoleName: String,
    games: List<GameInfo>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onGameLaunch: (GameInfo) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Game Library",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = consoleName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading games...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Failed to load games",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
        } else if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No games available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This console has no games installed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.title }) { game ->
                    GameCard(game = game, onClick = { onGameLaunch(game) })
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(bottomStart = 12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Launch",
                    modifier = Modifier.padding(6.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (game.platform.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = game.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
