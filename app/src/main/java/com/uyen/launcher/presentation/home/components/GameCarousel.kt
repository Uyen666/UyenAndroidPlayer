package com.uyen.launcher.presentation.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.data.model.GameCategory
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary

@Composable
fun GameCarousel(
    games: List<GameItem>,
    selectedIndex: Int,
    onSelectGame: (Int) -> Unit,
    onLaunchGame: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        itemsIndexed(games) { index, game ->
            val isSelected = index == selectedIndex

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.10f else 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "card_scale"
            )

            GameCard(
                game = game,
                isSelected = isSelected,
                scale = scale,
                onClick = {
                    if (isSelected) {
                        onLaunchGame(game)
                    } else {
                        onSelectGame(index)
                    }
                }
            )
        }
    }
}

@Composable
private fun GameCard(
    game: GameItem,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .scale(scale)
            .width(140.dp)
            .height(150.dp)
            .shadow(
                elevation = if (isSelected) 16.dp else 4.dp,
                shape = cardShape,
                ambientColor = if (isSelected) Ps5BlueGlow else Color.Black
            )
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    } else {
                        listOf(Color(0xFF131A29), Color(0xFF0B101D))
                    }
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(listOf(Ps5BlueGlow, SteamDeckAccent))
                } else {
                    Brush.linearGradient(listOf(SurfaceCardBorder, Color.Transparent))
                },
                shape = cardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 頂部小標誌
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (game.category) {
                            GameCategory.GALGAME -> Color(0xFF9333EA).copy(alpha = 0.25f)
                            GameCategory.RETRO -> Color(0xFFEA580C).copy(alpha = 0.25f)
                            GameCategory.STREAMING -> Color(0xFF0284C7).copy(alpha = 0.25f)
                            GameCategory.CUSTOM -> Color(0xFF16A34A).copy(alpha = 0.25f)
                            else -> Ps5Blue.copy(alpha = 0.25f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (game.category) {
                        GameCategory.GALGAME -> Icons.Default.MenuBook
                        GameCategory.RETRO -> Icons.Default.SportsEsports
                        GameCategory.STREAMING -> Icons.Default.Tv
                        GameCategory.CUSTOM -> Icons.Default.Widgets
                        else -> Icons.Default.Gamepad
                    },
                    contentDescription = null,
                    tint = if (isSelected) Color.White else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 遊戲名稱
            Text(
                text = game.title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            // 底部類別微標籤
            Text(
                text = game.category.displayName,
                fontSize = 10.sp,
                color = if (isSelected) SteamDeckAccent else TextMuted.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
