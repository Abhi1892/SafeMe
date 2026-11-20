package com.abhishek.appriskanalyzer.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val versionName: String,
    val installedDate: Long,
    val apkFilePath: String? = null,  // Only set for APK files from storage
    val isSystemApp: Boolean = false,
    val apkSizeFormatted: String? = null
)