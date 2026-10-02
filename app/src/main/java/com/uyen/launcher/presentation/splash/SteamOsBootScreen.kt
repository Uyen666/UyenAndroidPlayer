package com.uyen.launcher.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * Steam OS 風格開機/喚醒動畫畫面
 * 支援點擊任意處無縫跳過 (Tap anywhere to skip)
 */
@Composable
fun SteamOsBootScreen(
    onFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    // 旋轉動態粒子光環
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        // 淡入動畫
        alphaAnim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        // 預設播映時間 (2.5秒)，若使用者未點擊則自動進入主畫面
        delay(2200)
        alphaAnim.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 點擊即刻跳過
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // 背景微弱科技感動態弧光
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2.4f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Ps5Blue.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Ps5BlueGlow, SteamDeckAccent, AccentGold, Ps5BlueGlow)
                ),
                startAngle = pulseRotation.value,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Text(
                text = "UYEN OS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = TextPrimary
            )

            Text(
                text = "HANDHELD GAMING SYSTEM",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
                color = SteamDeckAccent
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "點擊任意處跳過動畫",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                modifier = Modifier.alpha(0.8f)
            )
        }

        // 底部版本標識
        Text(
            text = "Redmi 13C Edition • 90Hz Enabled",
            fontSize = 11.sp,
            color = TextMuted.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
