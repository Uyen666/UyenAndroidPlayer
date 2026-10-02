package com.uyen.launcher.presentation.minigame

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uyen.launcher.core.util.SoundManager
import com.uyen.launcher.presentation.theme.AccentGold
import com.uyen.launcher.presentation.theme.AccentGreen
import com.uyen.launcher.presentation.theme.BackgroundDark
import com.uyen.launcher.presentation.theme.GlassBackground
import com.uyen.launcher.presentation.theme.Ps5Blue
import com.uyen.launcher.presentation.theme.SteamDeckAccent
import com.uyen.launcher.presentation.theme.TextPrimary
import com.uyen.launcher.presentation.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

// 8-bit 子彈資料模型
data class Bullet(
    var x: Float,
    var y: Float,
    val vy: Float = -22f,
    val color: Color = Color(0xFF38BDF8)
)

// 8-bit 外星敵機資料模型
data class Invader(
    var x: Float,
    var y: Float,
    var vx: Float,
    val vy: Float,
    val size: Float = 36f,
    var hp: Int = 1,
    val type: Int = 0,
    val color: Color = Color(0xFFF43F5E)
)

// 像素爆炸粒子
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color
)

// 背景深空星點
data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

/**
 * UyenLauncher 內建 8-bit 太空突擊懷舊街機 (90Hz Smooth Canvas Mini-Game)
 */
@Composable
fun RetroArcadeScreen(
    soundManager: SoundManager,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("uyen_arcade_prefs", Context.MODE_PRIVATE) }

    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(prefs.getInt("high_score", 12500)) }
    var wave by remember { mutableIntStateOf(1) }
    var lives by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // 飛船位置 (正規化 0f ~ 1f)
    var shipX by remember { mutableFloatStateOf(0.5f) }
    var shipY by remember { mutableFloatStateOf(0.85f) }

    // 實體清單
    val bullets = remember { mutableStateListOf<Bullet>() }
    val invaders = remember { mutableStateListOf<Invader>() }
    val particles = remember { mutableStateListOf<Particle>() }
    val stars = remember {
        mutableStateListOf<Star>().apply {
            repeat(45) {
                add(
                    Star(
                        x = Random.nextFloat(),
                        y = Random.nextFloat(),
                        speed = Random.nextFloat() * 1.5f + 0.5f,
                        size = Random.nextFloat() * 2.5f + 1f,
                        alpha = Random.nextFloat() * 0.7f + 0.3f
                    )
                )
            }
        }
    }

    var spawnCounter by remember { mutableIntStateOf(0) }
    var isFiring by remember { mutableStateOf(false) }
    var fireCooldown by remember { mutableIntStateOf(0) }

    // 重新開始遊戲
    fun restartGame() {
        score = 0
        wave = 1
        lives = 3
        bullets.clear()
        invaders.clear()
        particles.clear()
        shipX = 0.5f
        shipY = 0.85f
        isGameOver = false
        isPaused = false
    }

    // 發射雷射音效與子彈
    fun fireLaser() {
        if (isGameOver || isPaused) return
        bullets.add(Bullet(x = shipX - 0.02f, y = shipY - 0.05f))
        bullets.add(Bullet(x = shipX + 0.02f, y = shipY - 0.05f))
        coroutineScope.launch { soundManager.playLaserSound() }
    }

    // 觸發爆炸粒子群
    fun triggerExplosion(x: Float, y: Float, color: Color) {
        repeat(16) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val speed = Random.nextFloat() * 6f + 2f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat(),
                    life = 1f,
                    maxLife = 1f,
                    color = color
                )
            )
        }
        coroutineScope.launch { soundManager.playExplosionSound() }
    }

    // 主遊戲循環 (90Hz 流暢幀率驅動)
    LaunchedEffect(isGameOver, isPaused) {
        while (!isGameOver && !isPaused) {
            withFrameNanos { _ ->
                // 1. 背景星空向下滾動
                stars.forEach { s ->
                    s.y += s.speed * 0.003f
                    if (s.y > 1f) {
                        s.y = 0f
                        s.x = Random.nextFloat()
                    }
                }

                // 2. 自動連射冷卻
                if (isFiring) {
                    if (fireCooldown <= 0) {
                        fireLaser()
                        fireCooldown = 10 // 約每 10 幀發射一輪
                    } else {
                        fireCooldown--
                    }
                } else {
                    if (fireCooldown > 0) fireCooldown--
                }

                // 3. 更新子彈位置
                val bulletIter = bullets.iterator()
                while (bulletIter.hasNext()) {
                    val b = bulletIter.next()
                    b.y += b.vy * 0.001f
                    if (b.y < -0.1f) {
                        bulletIter.remove()
                    }
                }

                // 4. 產生外星敵機
                spawnCounter++
                val spawnRate = (65 - (wave * 5)).coerceAtLeast(20)
                if (spawnCounter >= spawnRate) {
                    spawnCounter = 0
                    val type = Random.nextInt(3)
                    invaders.add(
                        Invader(
                            x = Random.nextFloat() * 0.8f + 0.1f,
                            y = -0.05f,
                            vx = (Random.nextFloat() - 0.5f) * 0.004f,
                            vy = (Random.nextFloat() * 0.003f + 0.002f) * (1f + wave * 0.15f),
                            hp = if (type == 2) 2 else 1,
                            type = type,
                            color = when (type) {
                                1 -> Color(0xFFA855F7)
                                2 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    )
                }

                // 5. 更新敵機移動與邊界反彈
                val invIter = invaders.iterator()
                while (invIter.hasNext()) {
                    val inv = invIter.next()
                    inv.x += inv.vx
                    inv.y += inv.vy
                    if (inv.x < 0.05f || inv.x > 0.95f) {
                        inv.vx = -inv.vx
                    }

                    // 碰撞檢測：敵機擊中玩家飛船
                    val dx = Math.abs(inv.x - shipX)
                    val dy = Math.abs(inv.y - shipY)
                    if (dx < 0.06f && dy < 0.05f) {
                        invIter.remove()
                        triggerExplosion(inv.x, inv.y, Color.Red)
                        lives--
                        if (lives <= 0) {
                            isGameOver = true
                            if (score > highScore) {
                                highScore = score
                                prefs.edit().putInt("high_score", score).apply()
                            }
                            coroutineScope.launch { soundManager.playGameOverSound() }
                        }
                        continue
                    }

                    // 飛出下邊界扣分或移除
                    if (inv.y > 1.05f) {
                        invIter.remove()
                    }
                }

                // 6. 碰撞檢測：子彈擊中敵機
                val hitBullets = mutableSetOf<Bullet>()
                val hitInvaders = mutableSetOf<Invader>()

                for (b in bullets) {
                    for (inv in invaders) {
                        val dx = Math.abs(b.x - inv.x)
                        val dy = Math.abs(b.y - inv.y)
                        if (dx < 0.05f && dy < 0.04f) {
                            hitBullets.add(b)
                            inv.hp--
                            if (inv.hp <= 0) {
                                hitInvaders.add(inv)
                                score += (inv.type + 1) * 100
                                if (score > highScore) {
                                    highScore = score
                                    prefs.edit().putInt("high_score", score).apply()
                                }
                                if (score % 2500 == 0) {
                                    wave++
                                    coroutineScope.launch { soundManager.playPowerUpSound() }
                                }
                            }
                            break
                        }
                    }
                }

                bullets.removeAll(hitBullets)
                hitInvaders.forEach { inv ->
                    triggerExplosion(inv.x, inv.y, inv.color)
                    invaders.remove(inv)
                }

                // 7. 更新粒子壽命與位置
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * 0.001f
                    p.y += p.vy * 0.001f
                    p.life -= 0.035f
                    if (p.life <= 0f) {
                        pIter.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // 主遊戲畫布 (繪製星空、飛船、外星怪、粒子、子彈、掃描線)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        shipX = (shipX + dragAmount.x / size.width).coerceIn(0.08f, 0.92f)
                        shipY = (shipY + dragAmount.y / size.height).coerceIn(0.2f, 0.9f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. 繪製深空背景星點
            stars.forEach { s ->
                drawCircle(
                    color = Color.White.copy(alpha = s.alpha),
                    radius = s.size,
                    center = Offset(s.x * w, s.y * h)
                )
            }

            // 2. 繪製子彈 (發光霓虹雙雷射)
            bullets.forEach { b ->
                val bx = b.x * w
                val by = b.y * h
                drawRect(
                    color = b.color,
                    topLeft = Offset(bx - 3f, by - 12f),
                    size = Size(6f, 24f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(bx, by)
                )
            }

            // 3. 繪製像素外星敵機 (8-bit Invader Block Style)
            invaders.forEach { inv ->
                val ix = inv.x * w
                val iy = inv.y * h
                val s = inv.size
                drawRetroInvader(ix, iy, s, inv.color, inv.type)
            }

            // 4. 繪製爆炸粒子
            particles.forEach { p ->
                val px = p.x * w
                val py = p.y * h
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = 3.5f * alpha,
                    center = Offset(px, py)
                )
            }

            // 5. 繪製玩家飛船 (8-bit Neon Starfighter)
            if (!isGameOver) {
                drawPlayerStarfighter(shipX * w, shipY * h)
            }

            // 6. 復古 CRT 掃描線渲染效果 (微暗條紋營造懷舊感)
            val scanlineCount = (h / 8f).toInt()
            for (i in 0 until scanlineCount) {
                val yLine = i * 8f
                drawLine(
                    color = Color.Black.copy(alpha = 0.18f),
                    start = Offset(0f, yLine),
                    end = Offset(w, yLine),
                    strokeWidth = 2f
                )
            }
        }

        // 頂部遊戲 HUD (分數、最高分、生命值、波次、離開)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 離開 / 返回啟動器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回啟動器",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "UYEN 8-BIT STRIKE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Ps5Blue,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 分數與最高分儀表板
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$score",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HI-SCORE", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$highScore",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentGold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAVE", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$wave",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 護甲 / 生命值 (心形計數)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(3) { idx ->
                        Text(
                            text = if (idx < lives) "❤️" else "🖤",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 1.dp)
                        )
                    }
                }
            }
        }

        // 底部觸控控制器 (左側虛擬搖桿 / 右側發射按鈕)
        if (!isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
            ) {
                // 左下角：八向虛擬方向搖桿
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.5.dp, SteamDeckAccent.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕹️", fontSize = 28.sp)
                    // 觸控拖曳監聽
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    shipX = (shipX + dragAmount.x / 400f).coerceIn(0.08f, 0.92f)
                                    shipY = (shipY + dragAmount.y / 400f).coerceIn(0.2f, 0.9f)
                                }
                            }
                    )
                }

                // 右下角：FIRE 巨型按鈕 + 連射開關
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 自動連射切換
                    Button(
                        onClick = { isFiring = !isFiring },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFiring) AccentGreen else GlassBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .border(1.dp, SteamDeckAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = if (isFiring) "AUTO: ON" else "AUTO: OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 單次發射 FIRE 大按鍵
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        fireLaser()
                                        tryAwaitRelease()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FIRE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 遊戲結束 Game Over 覆蓋彈窗
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xEE0B0E14))
                    .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "最終得分: $score",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "最高紀錄: $highScore",
                        fontSize = 14.sp,
                        color = AccentGold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Ps5Blue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("再次挑戰", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, SteamDeckAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Text("返回主頁", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 繪製玩家 8-bit 太空戰機
 */
private fun DrawScope.drawPlayerStarfighter(x: Float, y: Float) {
    val shipPath = Path().apply {
        moveTo(x, y - 24f) // 艦尖
        lineTo(x + 18f, y + 16f) // 右翼
        lineTo(x + 8f, y + 10f)
        lineTo(x, y + 14f) // 尾翼中心
        lineTo(x - 8f, y + 10f)
        lineTo(x - 18f, y + 16f) // 左翼
        close()
    }

    // 引擎噴射火焰
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = 5f,
        center = Offset(x, y + 15f)
    )

    // 機體繪製
    drawPath(
        path = shipPath,
        color = Color(0xFF0284C7)
    )
    drawPath(
        path = shipPath,
        color = Color(0xFF38BDF8),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
    )

    // 駕駛艙
    drawCircle(
        color = Color(0xFF67E8F9),
        radius = 4f,
        center = Offset(x, y - 4f)
    )
}

/**
 * 繪製 8-bit 復古侵略者外星怪
 */
private fun DrawScope.drawRetroInvader(x: Float, y: Float, size: Float, color: Color, type: Int) {
    val half = size / 2f
    val pixelSize = size / 7f

    // 幾何外星怪像素矩陣
    drawRect(
        color = color,
        topLeft = Offset(x - half, y - half),
        size = Size(size, size)
    )

    // 眼睛鏤空像素 (營造 8-bit 經典紅白機外星怪造型)
    drawRect(
        color = BackgroundDark,
        topLeft = Offset(x - half + pixelSize, y - half + pixelSize * 2),
        size = Size(pixelSize, pixelSize)
    )
    drawRect(
        color = BackgroundDark,
        topLeft = Offset(x + half - pixelSize * 2, y - half + pixelSize * 2),
        size = Size(pixelSize, pixelSize)
    )

    // 觸角
    drawRect(
        color = color,
        topLeft = Offset(x - half, y - half - pixelSize),
        size = Size(pixelSize, pixelSize)
    )
    drawRect(
        color = color,
        topLeft = Offset(x + half - pixelSize, y - half - pixelSize),
        size = Size(pixelSize, pixelSize)
    )
}
