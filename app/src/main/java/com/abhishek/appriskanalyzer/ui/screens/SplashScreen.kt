package com.abhishek.appriskanalyzer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.appriskanalyzer.R
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("INITIALIZING SECURITY KERNEL...") }

    // Outer cyber ring rotation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
        ),
        label = "radar_rotation",
    )

    // Inner counter-rotation
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
        ),
        label = "counter_rotation",
    )

    // Breathing pulse wave
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400, easing = LinearEasing),
        label = "splash_progress",
    )

    LaunchedEffect(Unit) {
        // Step 1: Initializing
        progress = 0.15f
        statusText = "INITIALIZING THREAT DATABASE..."
        delay(450.milliseconds)

        // Step 2: Heuristics
        progress = 0.40f
        statusText = "LOADING SECURITY HEURISTICS..."
        delay(500.milliseconds)

        // Step 3: Sandbox
        progress = 0.70f
        statusText = "PREPARING APPLICATION SANDBOX..."
        delay(550.milliseconds)

        // Step 4: Verification
        progress = 0.90f
        statusText = "ARMING RISK INTELLIGENCE..."
        delay(400.milliseconds)

        // Final Ready
        progress = 1.0f
        statusText = "SYSTEM READY"
        delay(300.milliseconds)

        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient background glowing gradient orb
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CyanAccent.copy(alpha = 0.12f),
                            Color(0xFF00E676).copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Hacker Doge Logo with Cyber Radar ─────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp),
            ) {
                // Outer pulsating halo
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.06f))
                        .border(1.dp, CyanAccent.copy(alpha = 0.25f), CircleShape),
                )

                // Rotating decorative outer tracker ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    CyanAccent,
                                    Color.Transparent,
                                    CyanAccent.copy(alpha = 0.6f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )

                // Counter-rotating inner dashed accent
                Box(
                    modifier = Modifier
                        .size(142.dp)
                        .rotate(counterRotation)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E676).copy(alpha = 0.8f),
                                    Color.Transparent,
                                    CyanAccent.copy(alpha = 0.5f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )

                // Core Doge Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.doge_hacker),
                        contentDescription = "SafeMe Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Title & Branding ──────────────────────────────────────
            Text(
                text = "SafeMe",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 1.5.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MOBILE SECURITY & RISK ANALYZER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Live Status Ticker & Cyber Progress ───────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Animated Status Text
                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "splash_status_anim",
                ) { targetStatus ->
                    Text(
                        text = targetStatus,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent.copy(alpha = 0.9f),
                        letterSpacing = 1.2.sp,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sleek Cyber Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DarkSurface),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        CyanAccent.copy(alpha = 0.6f),
                                        CyanAccent,
                                        Color(0xFF00E676),
                                    ),
                                ),
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Percentage and Engine Version
                Row(
                    modifier = Modifier.fillMaxWidth(0.75f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "SECURE KERNEL v1.0",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
