# 🛡️ SafeMe — Mobile Security & App Risk Analyzer

<p align="center">
  <img src="app/src/main/res/drawable/doge_hacker.png" width="160" height="160" alt="SafeMe Hacker Doge Logo" style="border-radius: 50%;" />
</p>

<p align="center">
  <b>On-Device Application Threat & Privacy Risk Intelligence Engine for Android</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose M3" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-informational?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-success?style=for-the-badge" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20On--Device-00E676?style=for-the-badge" alt="Privacy" />
</p>

---

## 📌 Overview

**SafeMe** is a mobile security auditor and vulnerability intelligence tool for Android. It evaluates installed applications and standalone `.apk` files by combining **deep static manifest/bytecode analysis** with **real-time dynamic system telemetry**.

SafeMe calculates a unified **0 – 100 Threat Vector Score** and categorizes applications into risk tiers (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), providing actionable recommendations to help users safeguard their personal data and device integrity.

> **Privacy First**: 100% of the analysis is conducted locally on the device. No telemetry, application metadata, or user details are ever transmitted to external servers.

---

## ✨ Key Features

### 🔍 1. Static Security Auditing
* **Debuggable Flags (`FLAG_DEBUGGABLE`)**: Detects apps left in debug mode, exposing memory and runtime states to reverse engineers.
* **Cleartext HTTP Traffic**: Identifies unencrypted network transmissions vulnerable to Man-In-The-Middle (MITM) snooping.
* **Legacy Target SDK (< API 28)**: Flags outdated applications that bypass modern Android runtime permissions and scoped storage safeguards.
* **Exported Components Audit**: Identifies Activities, Services, and Broadcast Receivers exposed to third-party applications without permission locks.
* **ADB USB Backup Exposure**: Checks if sensitive application data can be extracted via `adb backup` over USB without authentication.
* **Installation Source Verification**: Differentiates between trusted app store packages and unverified sideloaded binaries.
* **Dangerous Permissions Catalog**: Identifies broad access requests across Camera, Microphone, SMS, Contacts, Location, and Call Logs.

### ⚡ 2. Dynamic Behavioral Telemetry
* **Real Network Bandwidth (`NetworkStatsManager`)**: Measures exact 24-hour Mobile and Wi-Fi data traffic, broken down into Download (`rxMB`) and Upload (`txMB`).
* **Suspicious Data Exfiltration Detection**: Flags anomalous outbound data ratios (e.g. apps uploading excessive data in background states).
* **Screen Time vs. Background Execution (`UsageEvents`)**: Distinguishes between active user screen time and silent background service execution.
* **Process Importance & RAM Footprint (`ActivityManager`)**: Determines whether an app is running as an `IMPORTANCE_FOREGROUND_SERVICE`, `IMPORTANCE_SERVICE`, or `IMPORTANCE_CACHED`, while measuring live PSS memory consumption in MB.
* **Battery & Doze Mode Abuse (`PowerManager`)**: Checks if an app has requested exemption from Android's Doze mode battery optimization to stay awake indefinitely.
* **Special Access Auditing**: Scans for high-risk access rights such as Accessibility Services, Notification Listeners, and Device Administrator privileges.

### 📦 3. Standalone APK Scanner
* Directly select and analyze uninstalled `.apk` files from internal storage to audit security posture before installing them onto the operating system.

### 📊 4. Interactive Threat Intelligence Dashboard
* **0 – 100 Vulnerability Gauge**: Animated score meter and severity indicator.
* **Live Telemetry Grid**: Displays real device consumption metrics (MBs transferred, foreground minutes, background runtime, active services).
* **Permissions Inspector**: Visual tags categorized by hardware and privacy risk vectors (Camera, Mic, GPS, SMS, Contacts).
* **Actionable Advisory**: Context-aware security advice with a one-tap shortcut directly to Android's App Settings to revoke permissions or uninstall.
* **Shareable Report**: Export formatted plain-text security audits via the native Android share sheet.

---

## 🧮 Risk Scoring Methodology

SafeMe calculates a weighted risk score from **0 (Completely Safe)** to **100 (Critical Threat)**:

```
Total Risk Score (0 - 100) = Static Analysis Score (Max 60) + Dynamic Behavioral Score (Max 40)
```

| Domain | Finding | Score Contribution |
| :--- | :--- | :---: |
| **Static** | Debuggable Binary Enabled | `+15` |
| **Static** | Cleartext HTTP Allowed | `+10` |
| **Static** | Targets Outdated SDK (< 28) | `+8` |
| **Static** | Exported Components Exposed | `+7` |
| **Static** | ADB USB Backup Allowed | `+5` |
| **Static** | Installed from Untrusted Source | `+5` |
| **Static** | Dangerous Permissions Count | Up to `+20` |
| **Dynamic** | High Network Usage / Exfiltration Risk | `+15` |
| **Dynamic** | Persistent Background Execution | `+10` |
| **Dynamic** | Excessive Active Background Services (≥ 3) | `+8` |
| **Dynamic** | Elevated Runtime Permissions Granted (> 8) | `+8` |
| **Dynamic** | Battery Optimization / Doze Bypass | `+4` |

### Threat Tiers
* 🟢 **0 – 24 (LOW)**: Safe operational posture; adheres to modern Android security standards.
* 🟡 **25 – 49 (MEDIUM)**: Moderate privileges; permissions or background network activity should be reviewed.
* 🟠 **50 – 74 (HIGH)**: Elevated risk; extensive sensitive hardware permissions and network capabilities.
* 🔴 **75 – 100 (CRITICAL)**: Severe threat; immediate action recommended (revoke permissions or uninstall).

---

## 🏗️ Project Architecture

```
SafeMe/
├── app/
│   ├── build.gradle.kts           # Module configuration & dependencies
│   └── src/main/
│       ├── AndroidManifest.xml    # App manifest & permissions
│       ├── java/com/abhishek/appriskanalyzer/
│       │   ├── MainActivity.kt    # Entry activity & navigation crossfade
│       │   ├── analysis/
│       │   │   ├── StaticAnalyzer.kt   # Manifest, flags & permissions audit
│       │   │   ├── DynamicAnalyzer.kt  # NetworkStats, UsageEvents & telemetry
│       │   │   └── RiskScorer.kt       # 100-point threat scoring engine
│       │   ├── model/
│       │   │   ├── AppInfo.kt          # App metadata & APK model
│       │   │   ├── RiskLevel.kt        # Threat classification enum
│       │   │   └── ScanResult.kt       # Unified audit data container
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── SplashScreen.kt       # Cyber radar animation & status ticker
│       │       │   ├── AppSelectorScreen.kt  # Filter, search & APK file picker
│       │       │   ├── ScanningScreen.kt     # 5-phase live scan progression
│       │       │   └── ResultScreen.kt       # Threat report & telemetry dashboard
│       │       └── theme/                    # Cyberpunk dark color palette & typography
│       └── res/
│           ├── drawable/          # Vector drawables & doge_hacker.png
│           ├── mipmap-*/          # Launcher icons (Adaptive & Density mipmaps)
│           └── values/            # strings.xml, colors.xml, themes.xml
├── gradle/
│   └── libs.versions.toml         # Version catalog
└── settings.gradle.kts
```

---

## 🛠️ Tech Stack & Dependencies

* **Language**: [Kotlin](https://kotlinlang.org/) (2.0.21)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
* **Asynchronous Operations**: [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) (`Dispatchers.IO`)
* **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/) (2.5.0)
* **Build System**: Gradle 9.6 with Android Gradle Plugin (AGP 8.7.3)
* **Android SDK**: `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`

---

## 🔑 Permissions Used

| Permission | Purpose |
| :--- | :--- |
| `PACKAGE_USAGE_STATS` | Special access permission to monitor foreground screen time, background events, and network bandwidth via `NetworkStatsManager`. |
| `QUERY_ALL_PACKAGES` | Required on Android 11+ (API 30+) to enumerate installed applications for security auditing. |
| `INTERNET` | Used by Coil to load application icons when necessary. |
| `READ_PHONE_STATE` | Used to correlate carrier network socket identifiers during dynamic network queries. |

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Android Studio Ladybug (2024.2+) or newer.
* **JDK**: Java 11 or higher.
* **Android Device or Emulator**: Android 8.0 (API level 26) or higher.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Abhi1892/SafeMe.git
   cd SafeMe
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle dependencies:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run on a connected Android device or emulator.
5. On the first launch, grant **Usage Access** when prompted to enable real-time dynamic telemetry.

---

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are welcome!
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.

---

<p align="center">
  Developed by <b>Abhishek</b> • Built for mobile privacy & cybersecurity defense
</p>
