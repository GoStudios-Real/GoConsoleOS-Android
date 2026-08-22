package com.gostudios.console.watch

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

@Composable
fun DiscoveryScreen(
    hosts: Map<String, ConsoleHost>,
    onHostSelected: (ConsoleHost) -> Unit
) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        if (hosts.isEmpty()) {
            // Scanning state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D14)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        progress = 0.7f,
                        startAngle = 0f,
                        endAngle = 270f,
                        indicatorColor = Color(0xFF00C9DB),
                        trackColor = Color(0xFF2A2A44)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning...",
                        color = Color(0xFF8888AA),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Host list
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D14)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "GOCONSOLEOS",
                        color = Color(0xFF00C9DB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(hosts.values.toList()) { host ->
                    Card(
                        onClick = { onHostSelected(host) },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = Color(0xFF1A1A2E),
                            endBackgroundColor = Color(0xFF14141F)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = host.name,
                                color = Color(0xFFF0F0FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = host.address,
                                color = Color(0xFF8888AA),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}