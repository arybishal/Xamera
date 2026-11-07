# XAMERA — RELEASE BUILD GUIDE
## Reproducible Build & Packaging Instructions

**Application:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Version:** `1.0.0` (VersionCode: 17)  

---

## 1. Prerequisites & Toolchain

To ensure bit-for-bit build reproducibility, ensure the following toolchain versions are installed:

- **JDK:** OpenJDK 17 (Microsoft Build of OpenJDK 17.0.20+ or Eclipse Temurin 17)
- **Android SDK:** API 35 (Android 15) with Build-Tools `35.0.0`
- **Gradle:** 8.9 (managed via `./gradlew.bat` wrapper)
- **Android Gradle Plugin (AGP):** 8.7.2
- **Kotlin:** 2.0.21

---

## 2. Environment Setup

Set `JAVA_HOME` pointing to JDK 17 before invoking Gradle:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
```

Verify the environment:
```powershell
.\gradlew.bat --version
```

---

## 3. Building Release Artifacts

### 3.1 Running the Automated Test Baseline
Execute all 285 unit tests across the 62 suites:
```powershell
.\gradlew.bat testDebugUnitTest
```
Ensure 100% pass rate before assembling binaries.

### 3.2 Building Developer Debug APK
```powershell
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 3.3 Building Production-Signed Release APK
With local `keystore.properties` in place:
```powershell
.\gradlew.bat assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### 3.4 Automated Signing Script
If building on an unsigned checkout, execute the setup script:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup_release_signing.ps1
```

---

## 4. Checksum Verification

Compute SHA-256 hashes to verify build integrity against `RELEASE_CHECKSUMS.txt`:
```powershell
Get-FileHash -Algorithm SHA256 app\build\outputs\apk\release\app-release.apk
```
Expected SHA-256: `fdc2a4f0ef4350c82f99da3723ecd91a0ee8ff804b837f59d74b4b30ab6c91b9`
