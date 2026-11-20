package com.abhishek.appriskanalyzer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhishek.appriskanalyzer.analysis.DynamicAnalyzer
import com.abhishek.appriskanalyzer.analysis.RiskScorer
import com.abhishek.appriskanalyzer.analysis.StaticAnalyzer
import com.abhishek.appriskanalyzer.model.AppInfo
import com.abhishek.appriskanalyzer.model.ScanResult
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class ScanPhase(
    val phaseNumber: Int,
    val title: String,
    val icon: String
)

@Composable
fun ScanningScreen(appInfo: AppInfo, onScanComplete: (ScanResult) -> Unit) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Initializing Security Engine...") }
    var subText by remember { mutableStateOf("Preparing environment and sandbox telemetry...") }
    var progress by remember { mutableFloatStateOf(0.05f) }
    var currentPhase by remember { mutableIntStateOf(1) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val scanPhases = remember {
        listOf(
            ScanPhase(1, "Manifest", "📄"),
            ScanPhase(2, "Permissions", "🔑"),
            ScanPhase(3, "Behavior", "⚡"),
            ScanPhase(4, "System", "🔋"),
            ScanPhase(5, "Scoring", "🛡️")
        )
    }

    // Outer rotating radar ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing)
        ),
        label = "radar_ring"
    )

    // Counter-rotating inner ring
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing)
        ),
        label = "counter_radar"
    )

    // Concentric pulsating radar wave
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Smoothly animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "animated_progress"
    )

    LaunchedEffect(appInfo.packageName) {
        withContext(Dispatchers.IO) {
            try {
                // ── Phase 1: Manifest & Security Configurations ───────────────
                withContext(Dispatchers.Main) {
                    currentPhase = 1
                    statusText = "Auditing Manifest & Security Flags"
                    subText = "Inspecting backup permissions, cleartext HTTP & debug capabilities..."
                    progress = 0.15f
                }

                val staticResult = if (appInfo.apkFilePath != null) {
                    StaticAnalyzer(context).analyzeFromFile(appInfo.apkFilePath)
                } else {
                    StaticAnalyzer(context).analyze(appInfo.packageName)
                }

                withContext(Dispatchers.Main) {
                    progress = 0.35f
                }
                delay(350.milliseconds)

                // ── Phase 2: Permissions Audit ──────────────────────────────
                withContext(Dispatchers.Main) {
                    currentPhase = 2
                    statusText = "Permissions & Attack Surface"
                    subText = "Cross-referencing ${staticResult.dangerousPermissionsFound.size} sensitive permissions against attack vectors..."
                    progress = 0.50f
                }
                delay(400.milliseconds)

                // ── Phase 3: Dynamic Behavioral Telemetry ───────────────────
                withContext(Dispatchers.Main) {
                    currentPhase = 3
                    statusText = "Dynamic Telemetry & Network Analysis"
                    subText = "Inspecting data transfer ratios, background services & active sockets..."
                    progress = 0.65f
                }

                val dynamicResult = DynamicAnalyzer(context).analyze(appInfo.packageName)

                withContext(Dispatchers.Main) {
                    progress = 0.82f
                }
                delay(350.milliseconds)

                // ── Phase 4: Power, Doze & Process Footprint ────────────────
                withContext(Dispatchers.Main) {
                    currentPhase = 4
                    statusText = "System Resources & Doze Exemption"
                    subText = "Evaluating battery optimization bypass, RAM usage & process states..."
                    progress = 0.92f
                }
                delay(400.milliseconds)

                // ── Phase 5: Risk Scoring & Threat Vector Synthesis ─────────
                withContext(Dispatchers.Main) {
                    currentPhase = 5
                    statusText = "Synthesizing Risk Engine Score"
                    subText = "Aggregating findings into weighted vulnerability score..."
                    progress = 0.98f
                }
                delay(400.milliseconds)

                val finalResult = RiskScorer().score(appInfo, staticResult, dynamicResult)

                withContext(Dispatchers.Main) {
                    progress = 1.0f
                    delay(300.milliseconds)
                    onScanComplete(finalResult)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    scanError = e.message ?: "An unexpected error occurred during security analysis."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Target Application Card ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appInfo.icon != null) {
                            AsyncImage(
                                model = appInfo.icon,
                                contentDescription = appInfo.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("📱", fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = appInfo.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (appInfo.isSystemApp) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SystemBadgeColor.copy(alpha = 0.15f))
                                        .border(0.5.dp, SystemBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("SYSTEM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SystemBadgeColor)
                                }
                            }
                        }
                        Text(
                            text = appInfo.packageName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanDim)
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LIVE SCAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Radar Visual Centerpiece ──────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                // Pulsing outer ripple wave
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.05f))
                        .border(1.dp, CyanAccent.copy(alpha = 0.2f), CircleShape)
                )

                // Outer rotating radar ring
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(rotation),
                    color = CyanAccent.copy(alpha = 0.15f),
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round
                )

                // Counter-rotating decorative dashed ring
                CircularProgressIndicator(
                    progress = { 0.7f },
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(counterRotation),
                    color = CyanAccent.copy(alpha = 0.25f),
                    trackColor = Color.Transparent,
                    strokeWidth = 1.5.dp,
                    strokeCap = StrokeCap.Round
                )

                // Main progress ring
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(136.dp),
                    color = CyanAccent,
                    trackColor = DarkSurface,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )

                // Digital percentage inside center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "ANALYZING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5-Phase Pipeline Checklist ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                scanPhases.forEach { phase ->
                    val isDone = currentPhase > phase.phaseNumber
                    val isActive = currentPhase == phase.phaseNumber

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> CyanAccent.copy(alpha = 0.25f)
                                        isActive -> CyanAccent.copy(alpha = 0.15f)
                                        else -> DarkCard
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isDone || isActive -> CyanAccent
                                        else -> TextSecondary.copy(alpha = 0.2f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDone) "✓" else phase.icon,
                                fontSize = if (isDone) 14.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) CyanAccent else TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phase.title,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDone || isActive) CyanAccent else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Live Status Log Card with Smooth Animated Text ───────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .border(1.dp, CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = statusText,
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                        },
                        label = "status_animation"
                    ) { targetStatus ->
                        Text(
                            text = targetStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedContent(
                        targetState = subText,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "subtext_animation"
                    ) { targetSubText ->
                        Text(
                            text = targetSubText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Linear Glowing Progress Bar ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(CyanAccent.copy(alpha = 0.5f), CyanAccent)
                            )
                        )
                )
            }

            // Error display if scan failed
            if (scanError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E1015), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = scanError ?: "",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
