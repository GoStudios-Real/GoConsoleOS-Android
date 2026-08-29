package com.gostudios.console.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoAccent = Color(0xFF00C9DB)
private val GoDark = Color(0xFF0D1B2A)
private val GoSurface = Color(0xFF1B2838)

object Protocol {
    const val BTN_A = 0x130L
    const val BTN_B = 0x131L
    const val BTN_X = 0x133L
    const val BTN_Y = 0x134L
    const val BTN_START = 0x138L
    const val BTN_BACK = 0x139L
    const val BTN_TL = 0x136L
    const val BTN_TR = 0x137L
    const val ABS_X = 0x00L
    const val ABS_Y = 0x01L
    const val ABS_RX = 0x03L
    const val ABS_RY = 0x04L
}

@Composable
fun ControllerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onButton: (Long, Boolean) -> Unit,
    onAxis: (Long, Int) -> Unit,
) {
    if (!visible) return

    var buttons by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top row: triggers + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriggerButton("LT", onToggle = { onButton(Protocol.BTN_TL, it) })
                IconButton(onClick = onDismiss) {
                    Text("X", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                TriggerButton("RT", onToggle = { onButton(Protocol.BTN_TR, it) })
            }

            // Middle row: dpad - buttons - dpad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DPad(
                    onUp = { onAxis(Protocol.ABS_Y, -32767) },
                    onDown = { onAxis(Protocol.ABS_Y, 32767) },
                    onLeft = { onAxis(Protocol.ABS_X, -32767) },
                    onRight = { onAxis(Protocol.ABS_X, 32767) },
                    onRelease = { onAxis(Protocol.ABS_X, 0); onAxis(Protocol.ABS_Y, 0) },
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        SmallButton("Select") { onButton(Protocol.BTN_BACK, it) }
                        SmallButton("Start") { onButton(Protocol.BTN_START, it) }
                    }
                }

                FaceButtons(
                    onA = { onButton(Protocol.BTN_A, it) },
                    onB = { onButton(Protocol.BTN_B, it) },
                    onX = { onButton(Protocol.BTN_X, it) },
                    onY = { onButton(Protocol.BTN_Y, it) },
                )
            }

            // Bottom row: joysticks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Joystick("L", onMove = { x, y -> onAxis(Protocol.ABS_X, x); onAxis(Protocol.ABS_Y, y) })
                Joystick("R", onMove = { x, y -> onAxis(Protocol.ABS_RX, x); onAxis(Protocol.ABS_RY, y) })
            }
        }
    }
}

@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onRelease: () -> Unit,
) {
    val btnColor = Color.White.copy(alpha = 0.2f)
    val size = 44.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(btnColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onUp() },
                        onDragEnd = { onRelease() },
                        onDragCancel = { onRelease() },
                        onDrag = { _, _ -> },
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Text("^", color = Color.White, fontSize = 18.sp) }

        Row {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(btnColor)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onLeft() },
                            onDragEnd = { onRelease() },
                            onDragCancel = { onRelease() },
                            onDrag = { _, _ -> },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { Text("<", color = Color.White, fontSize = 18.sp) }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(btnColor)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onRight() },
                            onDragEnd = { onRelease() },
                            onDragCancel = { onRelease() },
                            onDrag = { _, _ -> },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { Text(">", color = Color.White, fontSize = 18.sp) }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(btnColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDown() },
                        onDragEnd = { onRelease() },
                        onDragCancel = { onRelease() },
                        onDrag = { _, _ -> },
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Text("v", color = Color.White, fontSize = 18.sp) }
    }
}

@Composable
private fun FaceButtons(
    onA: (Boolean) -> Unit,
    onB: (Boolean) -> Unit,
    onX: (Boolean) -> Unit,
    onY: (Boolean) -> Unit,
) {
    val size = 48.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SmallColorButton("Y", Color(0xFFFFD600)) { onY(it) }
        Row {
            SmallColorButton("X", Color(0xFF2196F3)) { onX(it) }
            Spacer(modifier = Modifier.width(8.dp))
            SmallColorButton("A", Color(0xFF4CAF50)) { onA(it) }
        }
        SmallColorButton("B", Color(0xFFF44336)) { onB(it) }
    }
}

@Composable
private fun SmallColorButton(
    label: String,
    color: Color,
    onToggle: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onToggle(true) },
                    onDragEnd = { onToggle(false) },
                    onDragCancel = { onToggle(false) },
                    onDrag = { _, _ -> },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallButton(
    label: String,
    onToggle: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onToggle(true) },
                    onDragEnd = { onToggle(false) },
                    onDragCancel = { onToggle(false) },
                    onDrag = { _, _ -> },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TriggerButton(
    label: String,
    onToggle: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onToggle(true) },
                    onDragEnd = { onToggle(false) },
                    onDragCancel = { onToggle(false) },
                    onDrag = { _, _ -> },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Joystick(
    label: String,
    onMove: (Int, Int) -> Unit,
) {
    val baseSize = 80.dp
    val knobSize = 36.dp
    val maxOffset = 22f

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        offsetX = 0f
                        offsetY = 0f
                        onMove(0, 0)
                    },
                    onDragCancel = {
                        offsetX = 0f
                        offsetY = 0f
                        onMove(0, 0)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffset, maxOffset)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-maxOffset, maxOffset)
                        onMove(
                            (offsetX / maxOffset * 32767).toInt(),
                            (offsetY / maxOffset * 32767).toInt(),
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { (offsetX / maxOffset * 20).toDp() },
                    y = with(LocalDensity.current) { (offsetY / maxOffset * 20).toDp() },
                )
                .size(knobSize)
                .clip(CircleShape)
                .background(GoAccent.copy(alpha = 0.8f))
                .border(2.dp, GoAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
