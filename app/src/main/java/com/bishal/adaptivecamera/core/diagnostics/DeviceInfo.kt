package com.bishal.adaptivecamera.core.diagnostics

import android.os.Build

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val product: String = Build.PRODUCT,
    val brand: String = Build.BRAND,
    val hardware: String = Build.HARDWARE,
    val androidVersion: String = Build.VERSION.RELEASE,
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val buildFingerprint: String = Build.FINGERPRINT,
    val displayBuildId: String = Build.DISPLAY,
    val isPrimaryTargetAce5: Boolean = isLikelyOnePlusAce5(Build.MODEL, Build.DEVICE, Build.PRODUCT)
) {
    companion object {
        fun isLikelyOnePlusAce5(model: String, device: String, product: String): Boolean {
            val terms = listOf(model, device, product).map { it.lowercase() }
            // OnePlus Ace 5 (China) model identifiers typically include PKG110 or PJR110 / ace5
            // Global 13R is CPH2645 / CPH2647 etc.
            return terms.any {
                it.contains("pkg110") || it.contains("ace 5") || it.contains("ace5")
            }
        }

        fun current(): DeviceInfo = DeviceInfo()
    }
}
