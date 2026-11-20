package com.abhishek.appriskanalyzer.analysis

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

class StaticAnalyzer(private val context: Context) {

    private val dangerousPermissions = listOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.GET_ACCOUNTS",
        "android.permission.USE_BIOMETRIC",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    data class StaticResult(
        val isDebuggable: Boolean,
        val allowsBackup: Boolean,
        val targetsSdkBelow28: Boolean,
        val allowsCleartextTraffic: Boolean,
        val hasExportedComponents: Boolean,
        val dangerousPermissionsFound: List<String>,
        val installedFromUnknownSource: Boolean
    )

    // Called for installed apps
    fun analyze(packageName: String): StaticResult {
        return try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS
            )
            val appInfo = packageInfo.applicationInfo!!
            analyzeFromInfo(appInfo, packageInfo, packageName, isFromFile = false)
        } catch (e: Exception) {
            defaultResult()
        }
    }

    // Called for APK files from storage
    fun analyzeFromFile(apkPath: String): StaticResult {
        return try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS
            )
            packageInfo?.applicationInfo?.sourceDir = apkPath
            packageInfo?.applicationInfo?.publicSourceDir = apkPath

            if (packageInfo == null) return defaultResult()

            val appInfo = packageInfo.applicationInfo!!
            analyzeFromInfo(appInfo, packageInfo, packageInfo.packageName, isFromFile = true)
        } catch (e: Exception) {
            defaultResult()
        }
    }

    private fun analyzeFromInfo(
        appInfo: ApplicationInfo,
        packageInfo: android.content.pm.PackageInfo,
        packageName: String,
        isFromFile: Boolean
    ): StaticResult {

        val pm = context.packageManager

        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val allowsBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        val targetsSdkBelow28 = appInfo.targetSdkVersion < 28
        val allowsCleartextTraffic = (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0

        val exportedActivities = packageInfo.activities?.any { it.exported } ?: false
        val exportedServices = packageInfo.services?.any { it.exported } ?: false
        val exportedReceivers = packageInfo.receivers?.any { it.exported } ?: false
        val hasExportedComponents = exportedActivities || exportedServices || exportedReceivers

        val appPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val foundDangerous = appPermissions.filter { perm ->
            dangerousPermissions.any { it.equals(perm, ignoreCase = true) }
        }

        // APK files from storage are always from unknown source
        val isFromUnknownSource = if (isFromFile) {
            true
        } else {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try { pm.getInstallSourceInfo(packageName).installingPackageName }
                catch (e: Exception) { null }
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            val trustedInstallers = listOf(
                "com.android.vending",
                "com.amazon.venezia",
                "com.samsung.android.packageinstaller"
            )
            installer == null || installer !in trustedInstallers
        }

        return StaticResult(
            isDebuggable = isDebuggable,
            allowsBackup = allowsBackup,
            targetsSdkBelow28 = targetsSdkBelow28,
            allowsCleartextTraffic = allowsCleartextTraffic,
            hasExportedComponents = hasExportedComponents,
            dangerousPermissionsFound = foundDangerous,
            installedFromUnknownSource = isFromUnknownSource
        )
    }

    // Safe fallback if analysis fails
    private fun defaultResult() = StaticResult(
        isDebuggable = false,
        allowsBackup = false,
        targetsSdkBelow28 = false,
        allowsCleartextTraffic = false,
        hasExportedComponents = false,
        dangerousPermissionsFound = emptyList(),
        installedFromUnknownSource = true
    )
}