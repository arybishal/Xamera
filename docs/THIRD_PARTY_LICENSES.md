# XAMERA — THIRD-PARTY LICENSES & OPEN-SOURCE NOTICES
## Open-Source Attribution & Dependency Inventory

**Application:** Xamera  
**Version:** `1.0.0` (Build 17)  
**Developer:** Bishal Aryal  

---

## 1. Core Open-Source Dependencies

Xamera is built upon leading open-source Android libraries, frameworks, and graphics standards:

### 1.1 Android Jetpack & Google Libraries
- **AndroidX Core KTX, Lifecycle, Activity, CameraX (`androidx.camera:*`), Room (`androidx.room:*`), Media3 (`androidx.media3:*`), DataStore (`androidx.datastore:*`)**  
  *License:* Apache License, Version 2.0  
  *Copyright:* The Android Open Source Project  
  *URL:* https://developer.android.com/jetpack  

### 1.2 Jetpack Compose & Material 3
- **Compose UI, Foundation, Material 3, Animation, Icons Extended (`androidx.compose.*`)**  
  *License:* Apache License, Version 2.0  
  *Copyright:* The Android Open Source Project  
  *URL:* https://developer.android.com/jetpack/compose  

### 1.3 Kotlin & Coroutines
- **Kotlin Standard Library, Kotlinx Coroutines Android**  
  *License:* Apache License, Version 2.0  
  *Copyright:* JetBrains s.r.o.  
  *URL:* https://kotlinlang.org  

### 1.4 Khronos OpenGL ES & EGL
- **OpenGL ES 3.0 / 3.2 APIs, EGL 1.4**  
  *Specification:* The Khronos Group Inc.  
  *URL:* https://www.khronos.org/opengles/  

---

## 2. Apache License Version 2.0 (Summary)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

---

## 3. FFmpeg Command Integration Notice

Xamera constructs compliant, standard CLI command arguments for post-processing and transcoding. In the default release APK, video export is executed on-device via Android's native hardware `MediaCodec` subsystem (`VideoExportEngine`), ensuring maximum performance, zero licensing conflict, and a minimal application package size (~46.7 MB).
