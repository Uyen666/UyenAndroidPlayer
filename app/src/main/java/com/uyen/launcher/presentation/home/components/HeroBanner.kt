package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = game,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "hero_anim",
        modifier = modifier
    ) { targetGame ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 類別標籤與標籤徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Ps5Blue.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = targetGame.category.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SteamDeckAccent
                        )
                    }

                    targetGame.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 遊戲大標題
                Text(
                    text = targetGame.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 副標題與遊玩時間
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetGame.subtitle,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    if (targetGame.playTimeHours > 0) {
                        Text(
                            text = " • 已遊玩 ${String.format("%.1f", targetGame.playTimeHours)} 小時",
                            fontSize = 12.sp,
                            color = AccentGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 開始遊戲大按鈕 (PS5 經典發光按鈕)
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ps5Blue
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "開始遊戲",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
