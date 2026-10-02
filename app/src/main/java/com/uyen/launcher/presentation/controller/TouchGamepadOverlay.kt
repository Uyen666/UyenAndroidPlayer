package com.uyen.launcher.presentation.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.core.controller.UyenControllerSender
import com.uyen.launcher.core.controller.VirtualGamepadState
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TouchGamepadOverlay(
    isFullScreenControllerMode: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val controllerSender = remember { UyenControllerSender() }
    var targetIp by remember { mutableStateOf("192.168.1.100") }
    var isConnected by remember { mutableStateOf(false) }

    var gamepadState by remember { mutableStateOf(VirtualGamepadState()) }

    fun sendUpdate(newState: VirtualGamepadState) {
        gamepadState = newState
        if (isConnected) {
            coroutineScope.launch {
                controllerSender.sendState(newState)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullScreenControllerMode) BackgroundDark else Color.Transparent)
    ) {
        // 頂部中置：PC 連線與控制面板
        if (isFullScreenControllerMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎮 UyenController PC 搖桿模式",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = targetIp,
                        onValueChange = { targetIp = it },
                        placeholder = { Text("PC IP 位址", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .width(150.dp)
                            .height(44.dp)
                    )
                    Button(
                        onClick = {
                            isConnected = !isConnected
                            if (isConnected) {
                                controllerSender.configure(targetIp)
                            } else {
                                controllerSender.close()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) AccentGreen else Ps5Blue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = if (isConnected) "已連線" else "連線 PC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "返回",
                        tint = TextSecondary
                    )
                }
            }
        }

        // 左側控制區：L1/L2 按鈕 + 虛擬搖桿 (Left Stick)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // L1 / L2 肩鍵
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShoulderButton(label = "L2", isPressed = gamepadState.btnL2 > 0.5f) { pressed ->
                        sendUpdate(gamepadState.copy(btnL2 = if (pressed) 1f else 0f))
                    }
                    ShoulderButton(label = "L1", isPressed = gamepadState.btnL1) { pressed ->
                        sendUpdate(gamepadState.copy(btnL1 = pressed))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 左類比搖桿
                VirtualJoystick(
                    onMove = { x, y ->
                        sendUpdate(gamepadState.copy(leftStickX = x, leftStickY = y))
                    }
                )
            }
        }

        // 右側控制區：R1/R2 肩鍵 + ABXY 菱形按鈕組
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // R1 / R2 肩鍵
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShoulderButton(label = "R1", isPressed = gamepadState.btnR1) { pressed ->
                        sendUpdate(gamepadState.copy(btnR1 = pressed))
                    }
                    ShoulderButton(label = "R2", isPressed = gamepadState.btnR2 > 0.5f) { pressed ->
                        sendUpdate(gamepadState.copy(btnR2 = if (pressed) 1f else 0f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ABXY 按鈕組
                AbxyCluster(
                    btnA = gamepadState.btnA,
                    btnB = gamepadState.btnB,
                    btnX = gamepadState.btnX,
                    btnY = gamepadState.btnY,
                    onButtonChange = { btn, isPressed ->
                        val updated = when (btn) {
                            "A" -> gamepadState.copy(btnA = isPressed)
                            "B" -> gamepadState.copy(btnB = isPressed)
                            "X" -> gamepadState.copy(btnX = isPressed)
                            "Y" -> gamepadState.copy(btnY = isPressed)
                            else -> gamepadState
                        }
                        sendUpdate(updated)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShoulderButton(
    label: String,
    isPressed: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) Ps5Blue else GlassBackground)
            .border(1.dp, SteamDeckAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.White else TextPrimary
        )
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val maxRadius = 45f

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(GlassBackground)
            .border(1.5.dp, SteamDeckAccent.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 搖桿手柄 (Thumb Knob)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(Ps5Blue)
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            offsetX = 0f
                            offsetY = 0f
                            onMove(0f, 0f)
                        },
                        onDragCancel = {
                            offsetX = 0f
                            offsetY = 0f
                            onMove(0f, 0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (offsetX + dragAmount.x).coerceIn(-maxRadius, maxRadius)
                            val newY = (offsetY + dragAmount.y).coerceIn(-maxRadius, maxRadius)
                            offsetX = newX
                            offsetY = newY
                            onMove(newX / maxRadius, newY / maxRadius)
                        }
                    )
                }
        )
    }
}

@Composable
private fun AbxyCluster(
    btnA: Boolean,
    btnB: Boolean,
    btnX: Boolean,
    btnY: Boolean,
    onButtonChange: (String, Boolean) -> Unit
) {
    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Y 按鈕 (頂)
        ActionButton(
            label = "Y",
            isPressed = btnY,
            color = Color(0xFFF59E0B),
            modifier = Modifier.align(Alignment.TopCenter)
        ) { onButtonChange("Y", it) }

        // A 按鈕 (底)
        ActionButton(
            label = "A",
            isPressed = btnA,
            color = Color(0xFF10B981),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { onButtonChange("A", it) }

        // X 按鈕 (左)
        ActionButton(
            label = "X",
            isPressed = btnX,
            color = Color(0xFF3B82F6),
            modifier = Modifier.align(Alignment.CenterStart)
        ) { onButtonChange("X", it) }

        // B 按鈕 (右)
        ActionButton(
            label = "B",
            isPressed = btnB,
            color = Color(0xFFEF4444),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) { onButtonChange("B", it) }
    }
}

@Composable
private fun ActionButton(
    label: String,
    isPressed: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onStateChange: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isPressed) color else GlassBackground)
            .border(1.5.dp, color.copy(alpha = 0.8f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isPressed) Color.White else color
        )
    }
}
