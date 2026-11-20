package com.abhishek.appriskanalyzer.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhishek.appriskanalyzer.R
import com.abhishek.appriskanalyzer.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ── Theme Colors ─────────────────────────────────────────────
val DarkBg = Color(0xFF0A0F1E)
val DarkSurface = Color(0xFF111827)
val DarkCard = Color(0xFF1A2235)
val CyanAccent = Color(0xFF00D4FF)
val CyanDim = Color(0xFF00D4FF).copy(alpha = 0.15f)
val TextPrimary = Color(0xFFEEF2FF)
val TextSecondary = Color(0xFF8B9CC8)
val SystemBadgeColor = Color(0xFFFF9100)

enum class AppFilter(val label: String, val icon: String) {
    USER("User", "👤"),
    SYSTEM("System", "⚙️"),
    ALL("All", "🌐")
}

enum class AppSort(val label: String) {
    NAME("A-Z"),
    RECENT("Recent")
}

@Composable
fun AppSelectorScreen(onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingApk by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf(AppFilter.USER) }
    var selectedSort by remember { mutableStateOf(AppSort.NAME) }
    var apkError by remember { mutableStateOf("") }

    // Function to reload apps asynchronously
    fun reloadApps() {
        isLoading = true
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) { loadInstalledApps(context) }
            allApps = list
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadApps()
    }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingApk = true
            apkError = ""
            coroutineScope.launch {
                val apkInfo = loadApkFromUri(context, uri)
                isProcessingApk = false
                if (apkInfo != null) {
                    onAppSelected(apkInfo)
                } else {
                    apkError = "Could not parse APK. Please ensure it is a valid Android package file."
                }
            }
        }
    }

    // Memoized filtered & sorted app list
    val displayedApps = remember(allApps, searchQuery, selectedFilter, selectedSort) {
        var filtered = when (selectedFilter) {
            AppFilter.USER -> allApps.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> allApps.filter { it.isSystemApp }
            AppFilter.ALL -> allApps
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }

        when (selectedSort) {
            AppSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            AppSort.RECENT -> filtered.sortedByDescending { it.installedDate }
        }
    }

    // Counts for filter chips
    val userCount = remember(allApps) { allApps.count { !it.isSystemApp } }
    val systemCount = remember(allApps) { allApps.count { it.isSystemApp } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Header Bar with Hacker Doge Logo ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSurface, DarkBg),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hacker Doge Logo Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            CyanAccent,
                                            Color(0xFF00E676),
                                            CyanAccent,
                                        ),
                                    ),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.doge_hacker),
                                contentDescription = "SafeMe Hacker Doge Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "SafeMe",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E676), CircleShape),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE THREAT MONITOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    letterSpacing = 1.2.sp,
                                )
                            }
                        }
                    }

                    // Refresh Button with cyber border
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), CircleShape)
                            .clickable { reloadApps() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔄", fontSize = 16.sp)
                    }
                }
            }

            // ── Quick Status Overview Cards ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Total Applications Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, TextSecondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📱", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Applications", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = "${allApps.size} Installed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                    }
                }

                // Security Shield Status Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, CyanAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Scanner Engine", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = "Ready to Audit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Navigation Tabs ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .background(DarkCard, RoundedCornerShape(14.dp))
                    .padding(4.dp),
            ) {
                listOf(
                    "Installed Apps" to "📱",
                    "Scan APK File" to "📦",
                ).forEachIndexed { index, (title, icon) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) CyanAccent.copy(alpha = 0.18f)
                                else Color.Transparent,
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) CyanAccent.copy(alpha = 0.6f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanAccent else TextSecondary,
                            )
                        }
                    }
                }
            }

            // ── Tab 0: Installed Apps ─────────────────────────────────
            when (selectedTab) {
                0 -> {
                    // Search Input with Clear Button
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search app name or package...", color = TextSecondary, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Text("🔍", fontSize = 14.sp)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(TextSecondary.copy(alpha = 0.2f))
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("✕", fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.25f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanAccent,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                        ),
                    )

                    // Filter and Sort Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Filter Pills with Icons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppFilter.entries.forEach { filter ->
                                val active = selectedFilter == filter
                                val count = when (filter) {
                                    AppFilter.USER -> userCount
                                    AppFilter.SYSTEM -> systemCount
                                    AppFilter.ALL -> allApps.size
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (active) CyanAccent.copy(alpha = 0.2f)
                                            else DarkCard,
                                        )
                                        .border(
                                            1.dp,
                                            if (active) CyanAccent.copy(alpha = 0.6f)
                                            else TextSecondary.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable { selectedFilter = filter }
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(filter.icon, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${filter.label} ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) CyanAccent else TextSecondary,
                                        )
                                    }
                                }
                            }
                        }

                        // Sort Toggle Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCard)
                                .border(1.dp, TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedSort = if (selectedSort == AppSort.NAME) AppSort.RECENT else AppSort.NAME
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "⇅ ${selectedSort.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CyanAccent,
                            )
                        }
                    }

                    // Content View
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = CyanAccent,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Indexing installed applications...",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    } else if (displayedApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text("🔍", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No applications found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty())
                                        "No results matching \"$searchQuery\" in ${selectedFilter.label} apps."
                                    else
                                        "No apps found in this category.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Text("Clear search", color = CyanAccent, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = displayedApps,
                                key = { it.packageName },
                            ) { app ->
                                AppListItem(
                                    app = app,
                                    onClick = { onAppSelected(app) },
                                )
                            }
                        }
                    }
                }

                // ── Tab 1: APK Scanner ────────────────────────────────
                1 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                        ) {
                            // Upload Box with Glowing Border
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(DarkSurface)
                                    .border(
                                        BorderStroke(
                                            width = 1.5.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    CyanAccent.copy(alpha = 0.7f),
                                                    CyanAccent.copy(alpha = 0.15f),
                                                ),
                                            ),
                                        ),
                                        RoundedCornerShape(20.dp),
                                    )
                                    .clickable {
                                        if (!isProcessingApk) {
                                            apkPicker.launch("application/vnd.android.package-archive")
                                        }
                                    }
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(CyanDim, CircleShape)
                                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("📦", fontSize = 34.sp)
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Scan Standalone APK",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap to pick an APK file from storage and inspect permissions, code security, and manifest risks before installing.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            if (!isProcessingApk) {
                                                apkPicker.launch("application/vnd.android.package-archive")
                                            }
                                        },
                                        enabled = !isProcessingApk,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanAccent.copy(alpha = 0.25f),
                                            disabledContainerColor = DarkCard,
                                        ),
                                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f)),
                                    ) {
                                        if (isProcessingApk) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = CyanAccent,
                                                strokeWidth = 2.dp,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Parsing APK...", color = CyanAccent, fontSize = 14.sp)
                                        } else {
                                            Text(
                                                text = "Browse Device Files",
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }

                            // Error display
                            AnimatedVisibility(
                                visible = apkError.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF2A1015), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                ) {
                                    Text(
                                        text = apkError,
                                        color = Color(0xFFFF8A80),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Features Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FeatureBadge(
                                    modifier = Modifier.weight(1f),
                                    icon = "🔍",
                                    title = "Permissions",
                                    desc = "Checks dangerous runtime accesses",
                                )
                                FeatureBadge(
                                    modifier = Modifier.weight(1f),
                                    icon = "🌐",
                                    title = "Cleartext HTTP",
                                    desc = "Detects unencrypted traffic",
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FeatureBadge(
                                    modifier = Modifier.weight(1f),
                                    icon = "🔓",
                                    title = "Exported APIs",
                                    desc = "Audits vulnerable components",
                                )
                                FeatureBadge(
                                    modifier = Modifier.weight(1f),
                                    icon = "⚙️",
                                    title = "Target SDK",
                                    desc = "Verifies modern OS protection",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    desc: String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, TextSecondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    val formattedDate = remember(app.installedDate) { formatDate(app.installedDate) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = TextSecondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center,
            ) {
                if (app.icon != null) {
                    AsyncImage(
                        model = app.icon,
                        contentDescription = "${app.name} icon",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("📱", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (app.isSystemApp) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SystemBadgeColor.copy(alpha = 0.15f))
                                .border(0.5.dp, SystemBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "SYSTEM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemBadgeColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "v${app.versionName}",
                        fontSize = 10.sp,
                        color = CyanAccent.copy(alpha = 0.8f),
                    )
                    if (formattedDate.isNotEmpty()) {
                        Text(
                            text = "•",
                            fontSize = 8.sp,
                            color = TextSecondary.copy(alpha = 0.5f),
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 10.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron
            Text(
                text = "›",
                fontSize = 20.sp,
                color = CyanAccent.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days <= 0L -> "Installed today"
        days == 1L -> "Installed yesterday"
        days < 30L -> "Installed ${days}d ago"
        days < 365L -> "Installed ${days / 30}mo ago"
        else -> "Installed ${days / 365}y ago"
    }
}

private fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(
                (PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS).toLong(),
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)
    }

    return packages.mapNotNull { packageInfo ->
        val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
        val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        val isSystemApp = isSystem && !isUpdatedSystem

        val name = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            appInfo.packageName
        }

        val icon = try {
            pm.getApplicationIcon(appInfo)
        } catch (_: Exception) {
            null
        }

        AppInfo(
            name = name,
            packageName = appInfo.packageName,
            icon = icon,
            versionName = packageInfo.versionName ?: "N/A",
            installedDate = packageInfo.firstInstallTime,
            isSystemApp = isSystemApp,
        )
    }
}

private suspend fun loadApkFromUri(context: Context, uri: Uri): AppInfo? = withContext(Dispatchers.IO) {
    try {
        val tempFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext null

        val fileSizeMb = tempFile.length() / (1024.0 * 1024.0)
        val formattedSize = "%.1f MB".format(fileSizeMb)

        val pm = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(tempFile.absolutePath, 0)
        }

        packageInfo?.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = tempFile.absolutePath
            appInfo.publicSourceDir = tempFile.absolutePath
        }

        packageInfo?.let {
            val label = it.applicationInfo?.loadLabel(pm)?.toString()
                ?: tempFile.nameWithoutExtension
            val icon = try { it.applicationInfo?.loadIcon(pm) } catch (_: Exception) { null }

            AppInfo(
                name = label,
                packageName = it.packageName,
                icon = icon,
                versionName = it.versionName ?: "1.0",
                installedDate = System.currentTimeMillis(),
                apkFilePath = tempFile.absolutePath,
                isSystemApp = false,
                apkSizeFormatted = formattedSize,
            )
        }
    } catch (_: Exception) {
        null
    }
}
