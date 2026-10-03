package com.uyen.launcher.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uyen.launcher.core.banner.BannerSourceType
import com.uyen.launcher.core.banner.ResolvedBanner
import com.uyen.launcher.core.util.AppIconUtil
import com.uyen.launcher.data.model.GameItem
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.Ps5BlueGlow
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.SurfaceCard
import com.uyen.launcher.presentation.theme.SurfaceCardBorder
import com.uyen.launcher.presentation.theme.TextMuted
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary

/**
 * 主機級遊戲快捷動作與海報客製化彈窗 (Game Actions & Banner Customizer)
 */
@Composable
fun GameActionMenuDialog(
    game: GameItem,
    resolvedBanner: ResolvedBanner?,
    isPinnedToHome: Boolean,
    isCustomBanner: Boolean,
    onPickCustomBanner: () -> Unit,
    onResetCustomBanner: () -> Unit,
    onReScrapePlayStore: () -> Unit,
    onTogglePinToHome: () -> Unit,
    onLaunchGame: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassBackground.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.25f),
                        SurfaceCardBorder.copy(alpha = 0.5f)
                    )
                )
            ),
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 頂部標題與關閉按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = game.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isPinnedToHome) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SteamDeckAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "★ 首頁",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SteamDeckAccent
                                    )
                                }
                            }
                        }

                        // 海報來源徽章
                        Spacer(modifier = Modifier.height(3.dp))
                        val source = resolvedBanner?.sourceType
                        val (badgeText, badgeColor) = when (source) {
                            BannerSourceType.CUSTOM_USER -> Pair("🎨 玩家自訂相片", Color(0xFFC084FC))
                            BannerSourceType.TV_BANNER -> Pair("📺 Android TV 官方橫幅", Color(0xFF38BDF8))
                            BannerSourceType.PLAY_STORE -> Pair("🌐 Google Play 宣傳圖", AccentGreen)
                            BannerSourceType.CURATED -> Pair("⭐ 官方精選海報", AccentGold)
                            else -> Pair("✨ 預設主題漸層", TextMuted)
                        }

                        Text(
                            text = "當前海報：$badgeText",
                            fontSize = 11.5.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 當前海報縮圖預覽卡片 (16:9 高畫質預覽)
                val cardShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(cardShape)
                        .background(AppIconUtil.getArtworkGradient(game))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), cardShape)
                ) {
                    if (resolvedBanner != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resolvedBanner.imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Poster Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // 縮圖底部微光陰影
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    Text(
                        text = if (resolvedBanner != null) "預覽：大海報視覺" else "預覽：主題漸層",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 操作按鈕列表
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    // 1. 從相簿更換自訂海報
                    Button(
                        onClick = {
                            onPickCustomBanner()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ps5Blue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "從相簿挑選自訂大海報",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. 還原為自動海報 (若當前為自訂相片才顯示)
                    if (isCustomBanner) {
                        OutlinedButton(
                            onClick = {
                                onResetCustomBanner()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "還原為自動抓取海報", fontSize = 12.5.sp)
                        }
                    }

                    // 3. 重新線上抓取 Play Store 宣傳圖 (若有安裝包名)
                    if (!game.packageName.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                onReScrapePlayStore()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SteamDeckAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "重新線上刮削 (Google Play 宣傳圖)", fontSize = 12.5.sp)
                        }
                    }

                    // 4. 釘選至首頁 / 從首頁移除切換
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onTogglePinToHome()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isPinnedToHome) AccentGold else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPinnedToHome) "從首頁移除" else "釘選到首頁",
                                fontSize = 12.sp,
                                color = if (isPinnedToHome) AccentGold else TextSecondary
                            )
                        }

                        // 5. 立即啟動遊戲
                        Button(
                            onClick = {
                                onDismiss()
                                onLaunchGame()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "啟動遊戲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
