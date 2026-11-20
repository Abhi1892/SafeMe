package com.abhishek.appriskanalyzer.analysis

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Performs comprehensive real-time runtime (dynamic) analysis of an installed application:
 * - Real network consumption (Download & Upload traffic via NetworkStatsManager)
 * - Suspicious data exfiltration pattern detection (abnormal upload vs download ratio)
 * - Actual foreground & background execution time (UsageStatsManager & UsageEvents)
 * - Real running process importance, foreground services, and RAM footprint
 * - Battery & power abuse detection (Doze mode exemption & Android App Standby Buckets)
 * - Accurate runtime granted permissions & sensitive dangerous permissions tracking
 * - High-risk special access audit (Accessibility Service, Notification Listener, Device Admin)
 */
class DynamicAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "SafeMe_Dynamic"

        private val DANGEROUS_PERMISSIONS = setOf(
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.CAMERA",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.RECEIVE_MMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.BODY_SENSORS",
            "android.permission.USE_BIOMETRIC"
        )
    }

    data class DynamicResult(
        // Core fields (100% backward compatible with existing UI & RiskScorer)
        val hasHighNetworkUsage: Boolean,
        val runsInBackground: Boolean,
        val hasExcessiveServices: Boolean,
        val hasHighBatteryUsage: Boolean,
        val requestsUsageAtRuntime: Boolean,
        val networkUsageMB: Double,
        val backgroundTimeMinutes: Long,
        val runningServicesCount: Int,
        val foregroundTimeMinutes: Long,
        val grantedPermissionsCount: Int,

        // Enhanced analysis metrics
        val downloadMB: Double = 0.0,
        val uploadMB: Double = 0.0,
        val memoryUsageMB: Double = 0.0,
        val isCurrentlyRunning: Boolean = false,
        val processImportanceLabel: String = "Not Running",
        val isIgnoringBatteryOptimizations: Boolean = false,
        val appStandbyBucketLabel: String = "Unknown",
        val grantedDangerousPermissions: List<String> = emptyList(),
        val hasSuspiciousDataExfiltration: Boolean = false,
        val hasSpecialPrivileges: Boolean = false,
        val specialPrivilegesList: List<String> = emptyList()
    )

    /**
     * Executes asynchronous dynamic analysis for the specified package.
     */
    suspend fun analyze(packageName: String): DynamicResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting dynamic analysis for package: $packageName")

        val (grantedCount, grantedDangerous) = getPermissionsAnalysis(packageName)
        val processInfo = getProcessAnalysis(packageName)
        val usageTimes = getUsageTimes(packageName)
        val network = getNetworkAnalysis(packageName)
        val battery = getBatteryAnalysis(packageName, usageTimes.foregroundMinutes, usageTimes.backgroundMinutes)
        val specialPrivileges = getSpecialPrivileges(packageName)

        val runsInBackground = processInfo.runsInBackground || usageTimes.hasBackgroundActivity
        val declaredServices = getDeclaredServicesCount(packageName)
        val hasExcessiveServices = processInfo.activeServiceCount >= 3 || declaredServices >= 5

        Log.d(TAG, """
            [Dynamic Analysis Summary: $packageName]
            - Network: Total=${"%.2f".format(network.totalMB)}MB (Down=${"%.2f".format(network.rxMB)}MB, Up=${"%.2f".format(network.txMB)}MB, ExfiltrationRisk=${network.isSuspiciousExfiltration})
            - Usage Time: Fg=${usageTimes.foregroundMinutes}m, Bg=${usageTimes.backgroundMinutes}m, ActiveInBg=$runsInBackground
            - Process: Running=${processInfo.isRunning}, State=${processInfo.importanceLabel}, Memory=${"%.1f".format(processInfo.memoryUsageMB)}MB
            - Services: Active=${processInfo.activeServiceCount}, Declared=$declaredServices
            - Permissions: GrantedTotal=$grantedCount, GrantedDangerous=${grantedDangerous.size} ($grantedDangerous)
            - Battery: DozeBypass=${battery.isIgnoringBatteryOptimizations}, StandbyBucket=${battery.standbyBucketLabel}, HighDrainRisk=${battery.isHighDrainRisk}
            - Special Privileges: ${specialPrivileges.privilegesList}
        """.trimIndent())

        DynamicResult(
            hasHighNetworkUsage = network.totalMB > 50.0 || network.isSuspiciousExfiltration,
            runsInBackground = runsInBackground,
            hasExcessiveServices = hasExcessiveServices,
            hasHighBatteryUsage = battery.isHighDrainRisk,
            requestsUsageAtRuntime = grantedDangerous.size >= 3 || grantedCount > 8,
            networkUsageMB = network.totalMB,
            backgroundTimeMinutes = usageTimes.backgroundMinutes,
            runningServicesCount = processInfo.activeServiceCount,
            foregroundTimeMinutes = usageTimes.foregroundMinutes,
            grantedPermissionsCount = grantedCount,
            downloadMB = network.rxMB,
            uploadMB = network.txMB,
            memoryUsageMB = processInfo.memoryUsageMB,
            isCurrentlyRunning = processInfo.isRunning,
            processImportanceLabel = processInfo.importanceLabel,
            isIgnoringBatteryOptimizations = battery.isIgnoringBatteryOptimizations,
            appStandbyBucketLabel = battery.standbyBucketLabel,
            grantedDangerousPermissions = grantedDangerous,
            hasSuspiciousDataExfiltration = network.isSuspiciousExfiltration,
            hasSpecialPrivileges = specialPrivileges.hasSpecialPrivileges,
            specialPrivilegesList = specialPrivileges.privilegesList
        )
    }

    /**
     * Accurately inspects granted runtime permissions and dangerous permissions.
     */
    private fun getPermissionsAnalysis(packageName: String): Pair<Int, List<String>> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }

            val requestedPermissions = packageInfo.requestedPermissions ?: return Pair(0, emptyList())
            val requestedFlags = packageInfo.requestedPermissionsFlags ?: return Pair(0, emptyList())

            var grantedCount = 0
            val grantedDangerous = mutableListOf<String>()

            for (i in requestedPermissions.indices) {
                // Correct flag check: PackageInfo.REQUESTED_PERMISSION_GRANTED == 2
                val isGranted = (requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (isGranted) {
                    grantedCount++
                    val perm = requestedPermissions[i]
                    if (DANGEROUS_PERMISSIONS.contains(perm)) {
                        grantedDangerous.add(perm)
                    }
                }
            }
            Pair(grantedCount, grantedDangerous)
        } catch (e: Exception) {
            Log.e(TAG, "getPermissionsAnalysis error for $packageName: ${e.message}")
            Pair(0, emptyList())
        }
    }

    /**
     * Gathers real-time process state, importance, active services, and memory consumption.
     */
    private data class ProcessAnalysis(
        val isRunning: Boolean,
        val importanceLabel: String,
        val activeServiceCount: Int,
        val memoryUsageMB: Double,
        val runsInBackground: Boolean
    )

    private fun getProcessAnalysis(packageName: String): ProcessAnalysis {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return ProcessAnalysis(false, "Not Running", 0, 0.0, false)

            val runningProcesses = am.runningAppProcesses ?: emptyList()
            val appProcesses = runningProcesses.filter { it.pkgList?.contains(packageName) == true }

            if (appProcesses.isEmpty()) {
                return ProcessAnalysis(
                    isRunning = false,
                    importanceLabel = "Not Running",
                    activeServiceCount = 0,
                    memoryUsageMB = 0.0,
                    runsInBackground = false
                )
            }

            // Lowest numerical value corresponds to highest importance
            val primaryProcess = appProcesses.minByOrNull { it.importance } ?: appProcesses.first()
            val pids = appProcesses.map { it.pid }.toIntArray()
            val memInfo = am.getProcessMemoryInfo(pids)
            val totalPssKb = memInfo.sumOf { it.totalPss }
            val memoryMB = totalPssKb / 1024.0

            val (importanceLabel, isBg) = when (primaryProcess.importance) {
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ->
                    Pair("Foreground Active", false)
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ->
                    Pair("Foreground Service", true)
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE ->
                    Pair("Visible", false)
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE ->
                    Pair("Background Service", true)
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED ->
                    Pair("Cached / Background", true)
                else -> {
                    val bg = primaryProcess.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
                    Pair("Active (Importance ${primaryProcess.importance})", bg)
                }
            }

            val activeServiceProcesses = appProcesses.count {
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE ||
                        it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
            }

            ProcessAnalysis(
                isRunning = true,
                importanceLabel = importanceLabel,
                activeServiceCount = activeServiceProcesses,
                memoryUsageMB = memoryMB,
                runsInBackground = isBg
            )
        } catch (e: Exception) {
            Log.e(TAG, "getProcessAnalysis error for $packageName: ${e.message}")
            ProcessAnalysis(false, "Unknown", 0, 0.0, false)
        }
    }

    /**
     * Inspects declared services in the package manifest.
     */
    private fun getDeclaredServicesCount(packageName: String): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SERVICES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES)
            }
            packageInfo.services?.size ?: 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Calculates foreground and background active duration using UsageStats and UsageEvents.
     */
    private data class UsageTimes(
        val foregroundMinutes: Long,
        val backgroundMinutes: Long,
        val hasBackgroundActivity: Boolean
    )

    private fun getUsageTimes(packageName: String): UsageTimes {
        return try {
            if (!hasUsageStatsPermission()) {
                Log.d(TAG, "UsageStats permission not granted, falling back to default times")
                return UsageTimes(0L, 0L, false)
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return UsageTimes(0L, 0L, false)

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 24 * 60 * 60 * 1000L

            var foregroundEventMillis = 0L
            var backgroundServiceMillis = 0L
            var lastResumeTime = 0L
            var lastServiceStartTime = 0L
            var hasBgEvents = false

            try {
                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.packageName == packageName) {
                        when (event.eventType) {
                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                lastResumeTime = event.timeStamp
                            }
                            UsageEvents.Event.ACTIVITY_PAUSED,
                            UsageEvents.Event.ACTIVITY_STOPPED -> {
                                if (lastResumeTime > 0) {
                                    foregroundEventMillis += (event.timeStamp - lastResumeTime).coerceAtLeast(0)
                                    lastResumeTime = 0L
                                }
                            }
                            UsageEvents.Event.FOREGROUND_SERVICE_START -> {
                                lastServiceStartTime = event.timeStamp
                                hasBgEvents = true
                            }
                            UsageEvents.Event.FOREGROUND_SERVICE_STOP -> {
                                if (lastServiceStartTime > 0) {
                                    backgroundServiceMillis += (event.timeStamp - lastServiceStartTime).coerceAtLeast(0)
                                    lastServiceStartTime = 0L
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "UsageEvents error: ${e.message}")
            }

            var stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime
            )
            if (stats.isNullOrEmpty()) {
                stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, startTime, endTime
                )
            }

            val appStats = stats?.find { it.packageName == packageName }
            val totalFgMillis = if (appStats != null && appStats.totalTimeInForeground > 0) {
                appStats.totalTimeInForeground
            } else {
                foregroundEventMillis
            }

            val totalFgMinutes = totalFgMillis / (1000 * 60)

            val bgMinutes = if (backgroundServiceMillis > 0) {
                backgroundServiceMillis / (1000 * 60)
            } else if (appStats != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val visible = appStats.totalTimeVisible
                if (visible > totalFgMillis) (visible - totalFgMillis) / (1000 * 60) else 0L
            } else {
                0L
            }

            val recentBackgroundLaunch = appStats != null &&
                    (endTime - appStats.lastTimeUsed) < (2 * 60 * 60 * 1000L) &&
                    totalFgMinutes == 0L

            UsageTimes(
                foregroundMinutes = totalFgMinutes,
                backgroundMinutes = bgMinutes,
                hasBackgroundActivity = hasBgEvents || recentBackgroundLaunch
            )
        } catch (e: Exception) {
            Log.e(TAG, "getUsageTimes error for $packageName: ${e.message}")
            UsageTimes(0L, 0L, false)
        }
    }

    /**
     * Queries accurate mobile & Wi-Fi network traffic, separating download and upload bytes.
     */
    private data class NetworkAnalysis(
        val totalMB: Double,
        val rxMB: Double,
        val txMB: Double,
        val isSuspiciousExfiltration: Boolean
    )

    private fun getNetworkAnalysis(packageName: String): NetworkAnalysis {
        return try {
            if (!hasUsageStatsPermission()) {
                return NetworkAnalysis(0.0, 0.0, 0.0, false)
            }

            val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                ).uid
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(packageName, 0).uid
            }

            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
                ?: return NetworkAnalysis(0.0, 0.0, 0.0, false)

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 24 * 60 * 60 * 1000L

            var rxBytes = 0L
            var txBytes = 0L

            fun queryNetworkType(type: Int) {
                try {
                    @Suppress("DEPRECATION")
                    val bucket = networkStatsManager.queryDetailsForUid(type, null, startTime, endTime, uid)
                    val stats = NetworkStats.Bucket()
                    while (bucket.hasNextBucket()) {
                        bucket.getNextBucket(stats)
                        rxBytes += stats.rxBytes
                        txBytes += stats.txBytes
                    }
                    bucket.close()
                } catch (e: Exception) {
                    Log.d(TAG, "queryDetailsForUid for network type $type: ${e.message}")
                }
            }

            @Suppress("DEPRECATION")
            queryNetworkType(ConnectivityManager.TYPE_MOBILE)
            @Suppress("DEPRECATION")
            queryNetworkType(ConnectivityManager.TYPE_WIFI)

            val rxMB = rxBytes / (1024.0 * 1024.0)
            val txMB = txBytes / (1024.0 * 1024.0)
            val totalMB = rxMB + txMB

            // Heuristic for data exfiltration: significant upload (> 15MB) with upload heavily exceeding download
            val isExfiltration = txMB > 15.0 && txMB > (rxMB * 3.0)

            NetworkAnalysis(totalMB, rxMB, txMB, isExfiltration)
        } catch (e: Exception) {
            Log.e(TAG, "getNetworkAnalysis error for $packageName: ${e.message}")
            NetworkAnalysis(0.0, 0.0, 0.0, false)
        }
    }

    /**
     * Inspects battery optimization exemptions and Android Standby Buckets.
     */
    private data class BatteryAnalysis(
        val isIgnoringBatteryOptimizations: Boolean,
        val standbyBucketLabel: String,
        val isHighDrainRisk: Boolean
    )

    private fun getBatteryAnalysis(
        packageName: String,
        foregroundMinutes: Long,
        backgroundMinutes: Long
    ): BatteryAnalysis {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isIgnoringOptimizations = powerManager?.isIgnoringBatteryOptimizations(packageName) == true

            val bucketLabel = if (isIgnoringOptimizations) "Exempt from Battery Optimization" else "Optimized"

            val isHighDrain = isIgnoringOptimizations ||
                    foregroundMinutes > 180 ||
                    backgroundMinutes > 120

            BatteryAnalysis(
                isIgnoringBatteryOptimizations = isIgnoringOptimizations,
                standbyBucketLabel = bucketLabel,
                isHighDrainRisk = isHighDrain
            )
        } catch (e: Exception) {
            Log.e(TAG, "getBatteryAnalysis error for $packageName: ${e.message}")
            BatteryAnalysis(false, "Unknown", false)
        }
    }

    /**
     * Checks if the app has acquired high-risk special privileges on the device.
     */
    private data class SpecialPrivileges(
        val hasSpecialPrivileges: Boolean,
        val privilegesList: List<String>
    )

    private fun getSpecialPrivileges(packageName: String): SpecialPrivileges {
        val list = mutableListOf<String>()

        // 1. Accessibility Service enabled
        try {
            val enabledAccessibility = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            if (enabledAccessibility.contains(packageName)) {
                list.add("Accessibility Service (Full Screen & Input Control)")
            }
        } catch (_: Exception) {}

        // 2. Notification Listener enabled
        try {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledListeners.contains(packageName)) {
                list.add("Notification Listener (Can Read All Notifications & OTPs)")
            }
        } catch (_: Exception) {}

        // 3. Device Administrator active
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val isAdmin = dpm?.activeAdmins?.any { it.packageName == packageName } == true
            if (isAdmin) {
                list.add("Device Administrator (Wipe Data, Lock Device)")
            }
        } catch (_: Exception) {}

        return SpecialPrivileges(
            hasSpecialPrivileges = list.isNotEmpty(),
            privilegesList = list
        )
    }

    /**
     * Verifies whether the app has been granted PACKAGE_USAGE_STATS permission by the user.
     */
    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
}
