package com.abhishek.appriskanalyzer.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhishek.appriskanalyzer.model.RiskLevel
import com.abhishek.appriskanalyzer.model.ScanResult

private enum class FindingFilter(val label: String) {
    ALL("All"),
    RISKS("Risks Only"),
    SAFE("Safe")
}

private data class AuditItem(
    val title: String,
    val isRisky: Boolean,
    val category: String, // "Static" or "Dynamic"
    val description: String,
    val riskDetail: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(result: ScanResult, onScanAnother: () -> Unit) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(FindingFilter.ALL) }

    val riskColor = when (result.riskLevel) {
        RiskLevel.LOW      -> Color(0xFF00E676)
        RiskLevel.MEDIUM   -> Color(0xFFFFD740)
        RiskLevel.HIGH     -> Color(0xFFFF6D00)
        RiskLevel.CRITICAL -> Color(0xFFFF1744)
    }

    val riskEmoji = when (result.riskLevel) {
        RiskLevel.LOW      -> "🛡️"
        RiskLevel.MEDIUM   -> "⚠️"
        RiskLevel.HIGH     -> "🚨"
        RiskLevel.CRITICAL -> "☠️"
    }

    // Animate score counting up
    val animatedScore by animateIntAsState(
        targetValue = result.riskScore,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "score"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = result.riskScore / 100f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "progress"
    )

    // Build structured audit items
    val allFindings = remember(result) {
        listOf(
            AuditItem(
                title = "Debuggable Binary",
                isRisky = result.isDebuggable,
                category = "Static",
                description = if (result.isDebuggable)
                    "Application is signed with debug flags enabled. Attackers can attach a debugger, inspect memory, and extract secrets."
                else "Production build with debugging disabled.",
                riskDetail = "High Vulnerability"
            ),
            AuditItem(
                title = "Cleartext HTTP Traffic",
                isRisky = result.allowsCleartextTraffic,
                category = "Static",
                description = if (result.allowsCleartextTraffic)
                    "App permits unencrypted HTTP connections, exposing user credentials and data to Man-In-The-Middle (MITM) snooping."
                else "Enforces TLS/HTTPS encryption on all network requests.",
                riskDetail = "Data Exposure"
            ),
            AuditItem(
                title = "Legacy Target SDK (< 28)",
                isRisky = result.targetsSdkBelow28,
                category = "Static",
                description = if (result.targetsSdkBelow28)
                    "Targets older Android SDK versions, bypassing modern Android security sandboxing, scoped storage, and privacy limits."
                else "Compliant with modern Android SDK security guidelines.",
                riskDetail = "Outdated Security"
            ),
            AuditItem(
                title = "Exported Components",
                isRisky = result.hasExportedComponents,
                category = "Static",
                description = if (result.hasExportedComponents)
                    "One or more Activities, Services, or Receivers are accessible to external third-party applications without permission locks."
                else "All sensitive components are properly internal or permission-gated.",
                riskDetail = "IPC Risk"
            ),
            AuditItem(
                title = "ADB USB Backup Allowed",
                isRisky = result.allowsBackup,
                category = "Static",
                description = if (result.allowsBackup)
                    "Application data can be exported via USB debug bridge (adb backup) without device owner authentication."
                else "App backup is restricted to secure cloud storage.",
                riskDetail = "Data Extraction"
            ),
            AuditItem(
                title = "Untrusted Installation Source",
                isRisky = result.installedFromUnknownSource,
                category = "Static",
                description = if (result.installedFromUnknownSource)
                    "Sideloaded from an external source rather than verified app marketplace."
                else "Verified package source.",
                riskDetail = "Unverified Origin"
            ),
            AuditItem(
                title = "Heavy Network Activity",
                isRisky = result.hasHighNetworkUsage,
                category = "Dynamic",
                description = if (result.hasHighNetworkUsage)
                    "Transferred ${"%.1f".format(result.networkUsageMB)} MB within the last 24 hours, exceeding normal background quotas."
                else "Network consumption is within normal limits (${"%.1f".format(result.networkUsageMB)} MB).",
                riskDetail = "Excessive Bandwidth"
            ),
            AuditItem(
                title = "Persistent Background Execution",
                isRisky = result.runsInBackground,
                category = "Dynamic",
                description = if (result.runsInBackground)
                    "App operates background services or launches unprompted when not in user view (${result.backgroundTimeMinutes}m background time)."
                else "Zero suspicious background executions detected.",
                riskDetail = "Background Activity"
            ),
            AuditItem(
                title = "Active Services Footprint",
                isRisky = result.hasExcessiveServices,
                category = "Dynamic",
                description = if (result.hasExcessiveServices)
                    "Runs ${result.runningServicesCount} background services simultaneously, keeping wake locks active."
                else "Service count is optimal (${result.runningServicesCount} active).",
                riskDetail = "Resource Drain"
            ),
            AuditItem(
                title = "Excessive Battery Drain",
                isRisky = result.hasHighBatteryUsage,
                category = "Dynamic",
                description = if (result.hasHighBatteryUsage)
                    "High power consumption pattern detected or app requests exemption from Android Doze mode."
                else "Power consumption is within normal operating parameters.",
                riskDetail = "Battery Drain"
            ),
            AuditItem(
                title = "Elevated Runtime Privileges",
                isRisky = result.requestsUsageAtRuntime,
                category = "Dynamic",
                description = if (result.requestsUsageAtRuntime)
                    "${result.grantedPermissionsCount} permissions currently granted, with access to sensitive hardware or data."
                else "Granted permissions count (${result.grantedPermissionsCount}) is balanced.",
                riskDetail = "Broad Permissions"
            )
        )
    }

    val riskyFindingsCount = remember(allFindings) { allFindings.count { it.isRisky } }
    val safeFindingsCount = remember(allFindings) { allFindings.count { !it.isRisky } }

    val filteredFindings = remember(allFindings, selectedFilter) {
        when (selectedFilter) {
            FindingFilter.ALL -> allFindings
            FindingFilter.RISKS -> allFindings.filter { it.isRisky }
            FindingFilter.SAFE -> allFindings.filter { !it.isRisky }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Top Header with Share Button ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyanDim, CircleShape)
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Security Audit Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "SafeMe Risk Intelligence Engine",
                            fontSize = 11.sp,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Share Report Action Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .border(1.dp, TextSecondary.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            try {
                                val shareBody = buildString {
                                    appendLine("🛡️ SafeMe App Risk Analysis")
                                    appendLine("Target: ${result.appInfo.name} (${result.appInfo.packageName})")
                                    appendLine("Threat Score: ${result.riskScore}/100 [${result.riskLevel.label}]")
                                    appendLine("Risks Flagged: $riskyFindingsCount | Checks Passed: $safeFindingsCount")
                                    appendLine("Dangerous Permissions: ${result.dangerousPermissions.size}")
                                    appendLine("Network: ${"%.1f".format(result.networkUsageMB)} MB | Background: ${result.backgroundTimeMinutes}m")
                                    appendLine("Generated by SafeMe Security Engine")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Security Audit"))
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📤", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Target Application Identity Card ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        if (result.appInfo.icon != null) {
                            AsyncImage(
                                model = result.appInfo.icon,
                                contentDescription = result.appInfo.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("📱", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = result.appInfo.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (result.appInfo.isSystemApp) {
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
                            text = result.appInfo.packageName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${result.appInfo.versionName}",
                            fontSize = 10.sp,
                            color = CyanAccent.copy(alpha = 0.8f)
                        )
                    }

                    // Open App Info button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .border(1.dp, TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", result.appInfo.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Settings ⚙️", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Threat Score Gauge Card ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                riskColor.copy(alpha = 0.18f),
                                DarkCard
                            )
                        )
                    )
                    .border(1.dp, riskColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = riskEmoji, fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = animatedScore.toString(),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = riskColor
                    )
                    Text(
                        text = "VULNERABILITY INDEX (0 - 100)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Threat level badge
                    Box(
                        modifier = Modifier
                            .background(riskColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(1.dp, riskColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${result.riskLevel.label} THREAT LEVEL",
                            color = riskColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Score Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(riskColor.copy(alpha = 0.6f), riskColor)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 SAFE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        Text("25 MODERATE", fontSize = 10.sp, color = Color(0xFFFFD740))
                        Text("50 HIGH", fontSize = 10.sp, color = Color(0xFFFF6D00))
                        Text("75+ CRITICAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Actionable Security Advisory ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when (result.riskLevel) {
                            RiskLevel.CRITICAL -> Color(0xFF2E1015)
                            RiskLevel.HIGH -> Color(0xFF2A1510)
                            RiskLevel.MEDIUM -> Color(0xFF282210)
                            RiskLevel.LOW -> Color(0xFF0D2418)
                        }
                    )
                    .border(
                        1.dp,
                        riskColor.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Security Recommendation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = riskColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (result.riskLevel) {
                            RiskLevel.CRITICAL ->
                                "Immediate action recommended. This application contains severe security vulnerabilities or persistent background monitoring. Consider uninstalling or isolating this app."
                            RiskLevel.HIGH ->
                                "Elevated risk profile. The app requests extensive sensitive device permissions and network privileges. Revoke unnecessary permissions in Android Settings."
                            RiskLevel.MEDIUM ->
                                "Moderate risk flags detected. Inspect background network usage and verify if requested permissions match expected app features."
                            RiskLevel.LOW ->
                                "Clean security posture. No severe architectural flaws, cleartext traffic, or abusive background processes were detected."
                        },
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Live Telemetry Grid ───────────────────────────────────
            SectionHeader(title = "Device Telemetry & Metrics", icon = "📊")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = "🌐",
                    title = "Network Usage",
                    value = "${"%.1f".format(result.networkUsageMB)} MB",
                    subtitle = "Last 24 hours"
                )
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = "📱",
                    title = "Screen Time",
                    value = "${result.foregroundTimeMinutes} min",
                    subtitle = "Active foreground"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = "⏱️",
                    title = "Background",
                    value = "${result.backgroundTimeMinutes} min",
                    subtitle = "Background active"
                )
                TelemetryCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚙️",
                    title = "Running Services",
                    value = "${result.runningServicesCount}",
                    subtitle = "Active daemons"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Dangerous Permissions Breakdown ───────────────────────
            if (result.dangerousPermissions.isNotEmpty()) {
                SectionHeader(
                    title = "Dangerous Permissions (${result.dangerousPermissions.size})",
                    icon = "🔑"
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    result.dangerousPermissions.forEach { perm ->
                        val cleanName = perm.replace("android.permission.", "")
                        val permIcon = when {
                            cleanName.contains("CAMERA") -> "📷"
                            cleanName.contains("AUDIO") -> "🎙️"
                            cleanName.contains("LOCATION") -> "📍"
                            cleanName.contains("CONTACT") -> "👥"
                            cleanName.contains("SMS") -> "✉️"
                            cleanName.contains("PHONE") || cleanName.contains("CALL") -> "📞"
                            cleanName.contains("STORAGE") || cleanName.contains("MEDIA") -> "📁"
                            cleanName.contains("CALENDAR") -> "📅"
                            cleanName.contains("BIOMETRIC") -> "🔒"
                            else -> "⚠️"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF281318))
                                .border(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(permIcon, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cleanName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF8A80)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Detailed Security Findings Section with Filter ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeader(title = "Audit Findings", icon = "📋")

                // Filter tabs
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FindingFilter.entries.forEach { filter ->
                        val active = selectedFilter == filter
                        val count = when (filter) {
                            FindingFilter.ALL -> allFindings.size
                            FindingFilter.RISKS -> riskyFindingsCount
                            FindingFilter.SAFE -> safeFindingsCount
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (active) CyanAccent.copy(alpha = 0.2f) else DarkCard
                                )
                                .border(
                                    0.5.dp,
                                    if (active) CyanAccent else TextSecondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) CyanAccent else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Findings List
            filteredFindings.forEach { item ->
                FindingCard(
                    label = item.title,
                    isRisky = item.isRisky,
                    category = item.category,
                    description = item.description,
                    riskDetail = item.riskDetail
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Bottom Action Button ──────────────────────────────────
            Button(
                onClick = onScanAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent.copy(alpha = 0.2f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(CyanAccent, CyanAccent.copy(alpha = 0.5f))
                    )
                )
            ) {
                Text(
                    text = "⟵  Scan Another Application",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(icon, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary
        )
    }
}

@Composable
private fun TelemetryCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    value: String,
    subtitle: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, TextSecondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = CyanAccent.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FindingCard(
    label: String,
    isRisky: Boolean,
    category: String,
    description: String = "",
    riskDetail: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = if (isRisky)
                    Color(0xFFFF5252).copy(alpha = 0.35f)
                else
                    TextSecondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isRisky) Color(0xFFFF5252) else Color(0xFF00E676),
                            CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        // Category Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = category.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }

                        // Risk tag
                        if (isRisky && (riskDetail != null)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = riskDetail,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Risk status badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isRisky) Color(0xFFFF5252).copy(alpha = 0.15f)
                            else Color(0xFF00E676).copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            0.5.dp,
                            if (isRisky) Color(0xFFFF5252).copy(alpha = 0.4f)
                            else Color(0xFF00E676).copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isRisky) "RISK" else "PASS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRisky) Color(0xFFFF5252) else Color(0xFF00E676),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Description text
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
