package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

@Composable
fun HeroBanner(
    game: GameItem,
    onLaunch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = game,
        transitionSpec = {
            (fadeIn(animationSpec = tween(280)) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ))
            .togetherWith(
                fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    targetOffsetY = { -16 }
                )
            )
        },
        label = "hero_anim",
        modifier = modifier
    ) { targetGame ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 6.dp)
        ) {
            // 精緻膠囊標籤列 (微毛玻璃半透明底色 + 細緻邊框)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = targetGame.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 0.2.sp
                    )
                }

                targetGame.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 遊戲大標題 (主機級 ExtraBold + 沉浸式陰影)
            Text(
                text = targetGame.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 2.5f),
                        blurRadius = 6f
                    )
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 副標題 (發行商 / 客戶端類型) 與遊玩時間
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = targetGame.subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.72f),
                    letterSpacing = 0.2.sp
                )
                if (targetGame.playTimeHours > 0) {
                    Text(
                        text = " • 已遊玩 ${String.format("%.1f", targetGame.playTimeHours)} 小時",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentGold
                    )
                }
            }
        }
    }
}
