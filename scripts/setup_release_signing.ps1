# Xamera Release Keystore Generation & APK Signing Script
# Package: com.bishal.adaptivecamera
# Developer: Bishal Aryal

param (
    [string]$KeystorePath = "$PSScriptRoot\..\xamera-release.jks",
    [string]$KeyAlias = "xamera-release",
    [string]$KeystorePassword = "",
    [string]$DName = "CN=Bishal Aryal, OU=Xamera, O=Adaptive Camera Project, L=Kathmandu, C=NP"
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$KeytoolPath = "$env:JAVA_HOME\bin\keytool.exe"
$ApksignerPath = "C:\Android\Sdk\build-tools\35.0.0\apksigner.bat"

if ([string]::IsNullOrWhiteSpace($KeystorePassword)) {
    # Generate secure 24-character random password if not passed
    $bytes = New-Object byte[] 18
    (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
    $KeystorePassword = [Convert]::ToBase64String($bytes)
}

# 1. Generate release keystore if it doesn't already exist
if (-not (Test-Path $KeystorePath)) {
    Write-Host "[+] Generating production RSA 2048-bit release keystore at: $KeystorePath" -ForegroundColor Cyan
    & $KeytoolPath -genkeypair -v `
        -keystore $KeystorePath `
        -alias $KeyAlias `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -storepass $KeystorePassword `
        -keypass $KeystorePassword `
        -dname $DName
    Write-Host "[+] Keystore successfully created." -ForegroundColor Green
} else {
    Write-Host "[*] Using existing release keystore at: $KeystorePath" -ForegroundColor Yellow
}

# 2. Write local git-ignored keystore.properties for Gradle builds
$PropertiesFile = "$PSScriptRoot\..\keystore.properties"
$PropsContent = @"
storeFile=xamera-release.jks
storePassword=$KeystorePassword
keyAlias=$KeyAlias
keyPassword=$KeystorePassword
"@
Set-Content -Path $PropertiesFile -Value $PropsContent
Write-Host "[+] Generated local keystore.properties (git-ignored)." -ForegroundColor Green

# 3. Sign the release APK if present
$UnsignedApk = "$PSScriptRoot\..\app\build\outputs\apk\release\app-release-unsigned.apk"
$SignedApk = "$PSScriptRoot\..\app\build\outputs\apk\release\app-release.apk"

if (Test-Path $UnsignedApk) {
    Write-Host "[+] Signing release APK with apksigner (v1, v2, v3 schemes)..." -ForegroundColor Cyan
    & $ApksignerPath sign `
        --ks $KeystorePath `
        --ks-key-alias $KeyAlias `
        --ks-pass "pass:$KeystorePassword" `
        --key-pass "pass:$KeystorePassword" `
        --v1-signing-enabled true `
        --v2-signing-enabled true `
        --v3-signing-enabled true `
        --out $SignedApk `
        $UnsignedApk

    Write-Host "[+] Verification of signed APK:" -ForegroundColor Green
    & $ApksignerPath verify --verbose --print-certs $SignedApk
} else {
    Write-Host "[!] Unsigned release APK not found at $UnsignedApk. Run ./gradlew.bat assembleRelease first." -ForegroundColor Yellow
}
