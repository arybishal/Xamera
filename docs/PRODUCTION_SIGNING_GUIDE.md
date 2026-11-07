# XAMERA — PRODUCTION SIGNING GUIDE
## Private Release Key Infrastructure & Maintenance Protocol

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Current Production Version:** `v1.0.0` (VersionCode: 17)  

---

## 1. Overview & Security Principles

Android requires all installed application packages to be digitally signed with a private cryptographic key. The private key establishes the developer's identity, protects update continuity, and prevents unauthorized application tampering.

### Critical Security Rules
- **NEVER commit the private keystore (`xamera-release.jks`) to Git.**
- **NEVER commit signing passwords or `keystore.properties` to source control.**
- Both `*.jks`, `*.keystore`, and `keystore.properties` are strictly registered in [.gitignore](file:///D:/Webapp/xamera/.gitignore).
- Keep a secure offline backup of `xamera-release.jks`. If the signing key is lost, future updates cannot be installed over existing installations without an uninstall and data loss.

---

## 2. Release Signing Identity Telemetry

The official production release identity established for Xamera v1.0.0 is:

- **Key Alias:** `xamera-release`
- **Key Algorithm:** RSA 2048-bit with SHA-256 Digest
- **Certificate Distinguished Name (DN):**
  `CN=Bishal Aryal, OU=Xamera, O=Adaptive Camera Project, L=Kathmandu, C=NP`
- **Certificate SHA-256 Digest:**
  `9f8f225a433040bdc1a9c868dc6e07b4203571cd7e63806ab6b1cde1b4af5d2b`
- **Certificate SHA-1 Digest:**
  `06c172559d52faf2f25a1193625ab5269b11fab5`
- **Validity:** 10,000 days (~27 years)

---

## 3. Local Configuration File (`keystore.properties`)

Gradle builds read release credentials from `keystore.properties` located in the project root directory:

```properties
storeFile=xamera-release.jks
storePassword=<SECURE_LOCAL_PASSWORD>
keyAlias=xamera-release
keyPassword=<SECURE_LOCAL_PASSWORD>
```

When `keystore.properties` is present, running `./gradlew.bat assembleRelease` automatically produces a production-signed, zipaligned APK at `app/build/outputs/apk/release/app-release.apk`.

If `keystore.properties` is omitted (e.g. on clean CI runners), Gradle safely produces `app-release-unsigned.apk` without failing the build.

---

## 4. Verification with Android SDK `apksigner`

To verify the signature integrity and certificate fingerprint of any release APK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
& "C:\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --verbose --print-certs "app\build\outputs\apk\release\app-release.apk"
```

Expected output:
```
Verifies
Verified using v3 scheme (APK Signature Scheme v3): true
Signer #1 certificate DN: CN=Bishal Aryal, OU=Xamera, O=Adaptive Camera Project, L=Kathmandu, C=NP
Signer #1 certificate SHA-256 digest: 9f8f225a433040bdc1a9c868dc6e07b4203571cd7e63806ab6b1cde1b4af5d2b
```

---

## 5. Keystore Disaster Recovery & Backup

1. **Cold Offline Storage:** Copy `xamera-release.jks` to a password-protected hardware flash drive stored in a secure location.
2. **Encrypted Vault Storage:** Store the base64-encoded keystore and password in an encrypted password manager (1Password, Bitwarden, KeePass).
3. **What happens if the key is lost?** Existing users cannot update the app without uninstalling the previous version first, which resets local app settings and requires manual backup of photos.
