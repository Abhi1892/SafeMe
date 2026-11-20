package com.abhishek.appriskanalyzer.model

data class ScanResult(
    val appInfo: AppInfo,

    // Static Analysis Findings
    val isDebuggable: Boolean = false,
    val allowsBackup: Boolean = false,
    val targetsSdkBelow28: Boolean = false,
    val allowsCleartextTraffic: Boolean = false,
    val hasExportedComponents: Boolean = false,
    val dangerousPermissions: List<String> = emptyList(),
    val installedFromUnknownSource: Boolean = false,

    // Dynamic Analysis Findings
    val hasHighNetworkUsage: Boolean = false,
    val runsInBackground: Boolean = false,
    val hasExcessiveServices: Boolean = false,
    val hasHighBatteryUsage: Boolean = false,
    val requestsUsageAtRuntime: Boolean = false,

    // Final Score
    val riskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val networkUsageMB: Double = 0.0,
    val backgroundTimeMinutes: Long = 0L,
    val runningServicesCount: Int = 0,
    val foregroundTimeMinutes: Long = 0L,
    val grantedPermissionsCount: Int = 0
)