package com.abhishek.appriskanalyzer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import com.abhishek.appriskanalyzer.model.AppInfo
import com.abhishek.appriskanalyzer.model.ScanResult
import com.abhishek.appriskanalyzer.ui.screens.AppSelectorScreen
import com.abhishek.appriskanalyzer.ui.screens.ResultScreen
import com.abhishek.appriskanalyzer.ui.screens.ScanningScreen
import com.abhishek.appriskanalyzer.ui.screens.SplashScreen
import com.abhishek.appriskanalyzer.ui.theme.AppRiskAnalyzerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for Usage Access permission on first launch
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        setContent {
            AppRiskAnalyzerTheme {
                AppNavigation()
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE)
                    as android.app.usage.UsageStatsManager
            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                System.currentTimeMillis() - 1000,
                System.currentTimeMillis(),
            )
            (stats != null) && stats.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "navigation_crossfade",
    ) { screen ->
        when (screen) {
            "splash" -> SplashScreen(
                onSplashFinished = {
                    currentScreen = "selector"
                }
            )
            "selector" -> AppSelectorScreen(
                onAppSelected = { app ->
                    selectedApp = app
                    currentScreen = "scanning"
                },
            )
            "scanning" -> ScanningScreen(
                appInfo = selectedApp!!,
                onScanComplete = { result ->
                    scanResult = result
                    currentScreen = "result"
                },
            )
            "result" -> ResultScreen(
                result = scanResult!!,
                onScanAnother = {
                    currentScreen = "selector"
                },
            )
        }
    }
}
