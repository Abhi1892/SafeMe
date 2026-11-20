package com.abhishek.appriskanalyzer.analysis

import com.abhishek.appriskanalyzer.model.AppInfo
import com.abhishek.appriskanalyzer.model.RiskLevel
import com.abhishek.appriskanalyzer.model.ScanResult

class RiskScorer {

    fun score(
        appInfo: AppInfo,
        static: StaticAnalyzer.StaticResult,
        dynamic: DynamicAnalyzer.DynamicResult
    ): ScanResult {

        var score = 0

        // ── Static Scoring (max 60 points) ──────────────────────────
        if (static.isDebuggable)              score += 15
        if (static.allowsCleartextTraffic)    score += 10
        if (static.targetsSdkBelow28)         score += 8
        if (static.hasExportedComponents)     score += 7
        if (static.allowsBackup)              score += 5
        if (static.installedFromUnknownSource) score += 5

        // More dangerous permissions = higher score
        score += when (static.dangerousPermissionsFound.size) {
            in 8..Int.MAX_VALUE -> 20
            in 5..7             -> 15
            in 3..4             -> 10
            in 1..2             -> 5
            else                -> 0
        }

        // ── Dynamic Scoring (max 40 points) ─────────────────────────
        if (dynamic.hasHighNetworkUsage)    score += 15
        if (dynamic.runsInBackground)       score += 10
        if (dynamic.hasExcessiveServices)   score += 8
        if (dynamic.requestsUsageAtRuntime) score += 8
        if (dynamic.hasHighBatteryUsage)    score += 4

        // Cap at 100
        score = score.coerceAtMost(100)

        // Determine risk level
        val riskLevel = when {
            score >= 75 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 25 -> RiskLevel.MEDIUM
            else        -> RiskLevel.LOW
        }

        return ScanResult(
            appInfo = appInfo,
            isDebuggable = static.isDebuggable,
            allowsBackup = static.allowsBackup,
            targetsSdkBelow28 = static.targetsSdkBelow28,
            allowsCleartextTraffic = static.allowsCleartextTraffic,
            hasExportedComponents = static.hasExportedComponents,
            dangerousPermissions = static.dangerousPermissionsFound,
            installedFromUnknownSource = static.installedFromUnknownSource,
            hasHighNetworkUsage = dynamic.hasHighNetworkUsage,
            runsInBackground = dynamic.runsInBackground,
            hasExcessiveServices = dynamic.hasExcessiveServices,
            hasHighBatteryUsage = dynamic.hasHighBatteryUsage,
            requestsUsageAtRuntime = dynamic.requestsUsageAtRuntime,
            riskScore = score,
            riskLevel = riskLevel,
            networkUsageMB = dynamic.networkUsageMB,
            backgroundTimeMinutes = dynamic.backgroundTimeMinutes,
            runningServicesCount = dynamic.runningServicesCount,
            foregroundTimeMinutes = dynamic.foregroundTimeMinutes,
            grantedPermissionsCount = dynamic.grantedPermissionsCount
        )
    }
}