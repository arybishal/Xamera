# XAMERA — PRIVACY & SECURITY AUDIT
## On-Device Architecture, Permission Rationalization & Data Integrity

**Application:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Version:** `1.0.0` (VersionCode: 17)  
**Date:** 2026-11-07  

---

## 1. Core Architectural Privacy Principle

**Xamera is strictly an on-device application.**

- **No Remote Network Requests:** Xamera contains zero network communication code. It does not ping home servers, query telemetry endpoints, or send diagnostics over the internet.
- **Zero Third-Party Trackers or Analytics:** The application includes no Google Analytics, Firebase Analytics, Crashlytics, Facebook SDK, or advertising networks.
- **No Account Requirements:** No logins, registrations, API keys, or subscriptions are required or requested.
- **Local Media Storage:** All photos and videos remain exclusively on the user's device under standard Android MediaStore directories (`DCIM/Xamera`).

---

## 2. Android Permissions Audit & Rationalization

| Permission | Protection Level | Purpose & Technical Necessity | Gating & Runtime User Control |
|---|:---:|---|---|
| `android.permission.CAMERA` | Dangerous | Essential for acquiring live sensor frames, viewfinder preview, photo capture, and video recording. | Requested at first app launch; blocked preview screen with clear explanation if denied. |
| `android.permission.RECORD_AUDIO` | Dangerous | Required for capturing synchronized AAC/PCM audio tracks during video recording. | Requested when switching to Video mode or triggering recording. Video can still be recorded muted if denied. |
| `android.permission.ACCESS_FINE_LOCATION` | Dangerous | Optional. Allows injecting GPS coordinates into EXIF photo tags when Geotagging is explicitly enabled. | Strictly optional. Never requested at launch; only requested if user turns ON "EXIF Geotagging" in Settings. |
| `android.permission.ACCESS_COARSE_LOCATION`| Dangerous | Optional. Fallback for approximate location geotagging. | Same as FINE_LOCATION; completely inert when geotagging is disabled. |
| `android.permission.BLUETOOTH_CONNECT` | Dangerous (API 31+) | Required for pairing and receiving shutter clicks from external Bluetooth remote shutters and gimbals. | Only requested when the user opens "Bluetooth Remote Setup" in Settings. |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (API 33+) | Notifies the user when long-running video transcoding or computational stacking jobs finish in the background. | Gracefully falls back to in-app banners if notification permission is denied. |
| `android.permission.VIBRATE` | Normal | Provides haptic feedback on shutter button release, zoom dial tick marks, and horizon level lock. | Can be disabled anytime via the "Haptic Feedback" toggle in Settings. |

---

## 3. EXIF Privacy & Safe Sharing

Digital photographs often encode sensitive personal data, such as GPS coordinates, camera serial numbers, and capture timestamps. Xamera provides robust built-in privacy protections:

1. **EXIF Privacy Switch:** Available in Settings ("EXIF Privacy Strip"). When activated, GPS latitude/longitude, camera serial numbers, and device unique identifiers are omitted from output JPEGs.
2. **In-App Safe Sharing (`FEAT-INT-08`):** When sharing photos or videos from the in-app gallery to third-party social apps or messaging clients, Xamera allows stripping metadata on-the-fly, generating a scrubbed temporary file for the share intent.
3. **Temporary Cache Cleanup:** Scrubbed temporary sharing files stored in `context.cacheDir` are automatically purged upon activity destruction or background transition.

---

## 4. Secret Protection & Repository Hygiene

- **Keystore Isolation:** The private signing keystore `xamera-release.jks` and credentials file `keystore.properties` are listed in `.gitignore`.
- **Zero Hardcoded Secrets:** No API keys, passwords, private certificates, or internal server tokens exist in the source code or Git history.
