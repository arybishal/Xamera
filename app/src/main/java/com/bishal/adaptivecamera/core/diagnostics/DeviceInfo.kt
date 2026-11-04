package com.bishal.adaptivecamera.core.diagnostics

import android.os.Build

enum class PhysicalIdentityConfidence {
    CONFIRMED,
    PROBABLE,
    UNCONFIRMED,
    CONFLICTING
}

data class DeviceIdentityEvidence(
    val factor: String,
    val observation: String,
    val implication: String
)

data class DeviceInfo(
    val reportedManufacturer: String = Build.MANUFACTURER,
    val reportedModel: String = Build.MODEL,
    val reportedDevice: String = Build.DEVICE,
    val reportedProduct: String = Build.PRODUCT,
    val reportedBrand: String = Build.BRAND,
    val reportedHardware: String = Build.HARDWARE,
    val androidVersion: String = Build.VERSION.RELEASE,
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val buildFingerprint: String = Build.FINGERPRINT,
    val displayBuildId: String = Build.DISPLAY,

    val firmwareIdentity: String,
    val probableHardwareFamily: String,
    val physicalHardwareIdentity: String,
    val physicalIdentityConfidence: PhysicalIdentityConfidence,
    val identityEvidence: List<DeviceIdentityEvidence> = emptyList()
) {
    // Backward compatibility helpers for UI / Reports
    val manufacturer: String get() = reportedManufacturer
    val model: String get() = reportedModel
    val device: String get() = reportedDevice
    val product: String get() = reportedProduct
    val brand: String get() = reportedBrand
    val hardware: String get() = reportedHardware

    companion object {
        fun analyze(
            manufacturer: String = Build.MANUFACTURER,
            model: String = Build.MODEL,
            device: String = Build.DEVICE,
            product: String = Build.PRODUCT,
            brand: String = Build.BRAND,
            hardware: String = Build.HARDWARE,
            fingerprint: String = Build.FINGERPRINT,
            display: String = Build.DISPLAY,
            androidVersion: String = Build.VERSION.RELEASE,
            apiLevel: Int = Build.VERSION.SDK_INT
        ): DeviceInfo {
            val evidence = mutableListOf<DeviceIdentityEvidence>()
            val m = model.trim()
            val d = device.trim()
            val p = product.trim()
            val fp = fingerprint.trim()

            // 1. Firmware Identity Analysis
            val fwIdentity = when {
                p.contains("CPH2691IN", ignoreCase = true) || m.contains("CPH2691", ignoreCase = true) -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Firmware Build Identifier",
                            observation = "Model '$m', Product '$p', Fingerprint '$fp'",
                            implication = "Firmware identity corresponds to OnePlus 13R India release build (CPH2691IN)"
                        )
                    )
                    "OnePlus 13R (India / Regional CPH2691IN)"
                }
                m.contains("CPH2645", ignoreCase = true) || m.contains("CPH2647", ignoreCase = true) -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Firmware Build Identifier",
                            observation = "Model '$m'",
                            implication = "Firmware identity corresponds to global OnePlus 13R model designation"
                        )
                    )
                    "OnePlus 13R (Global / International)"
                }
                m.contains("PKG110", ignoreCase = true) || m.contains("PJR110", ignoreCase = true) -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Firmware Build Identifier",
                            observation = "Model '$m'",
                            implication = "Firmware identity corresponds to Chinese factory release for OnePlus Ace 5"
                        )
                    )
                    "OnePlus Ace 5 (China Factory PKG110)"
                }
                else -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Firmware Build Identifier",
                            observation = "$brand $model ($product)",
                            implication = "Generic or standard OEM firmware build"
                        )
                    )
                    "$brand $model"
                }
            }

            // 2. Hardware Platform Family Analysis
            val hwFamily: String
            val isAce5Or13RFamily = d.equals("OP5D3BL1", ignoreCase = true) ||
                    d.equals("PKG110", ignoreCase = true) ||
                    m.contains("CPH2691", ignoreCase = true) ||
                    m.contains("CPH2645", ignoreCase = true) ||
                    m.contains("PKG110", ignoreCase = true)

            if (isAce5Or13RFamily) {
                evidence.add(
                    DeviceIdentityEvidence(
                        factor = "Board Hardware Platform",
                        observation = "Device '$d', Hardware '$hardware'",
                        implication = "Belongs to the shared OnePlus Ace 5 / OnePlus 13R Qualcomm SM8650 platform family"
                    )
                )
                hwFamily = "OnePlus Ace 5 / 13R Platform (Board: $d)"
            } else {
                hwFamily = "$brand $hardware platform"
            }

            // 3. Physical Hardware Identity & Confidence Determination
            val physicalId: String
            val confidence: PhysicalIdentityConfidence

            when {
                // Flash / Converted ROM case:
                // Board OP5D3BL1 with Indian firmware CPH2691IN.
                // Device may physically be a Chinese Ace 5 flashed with Indian OxygenOS/ColorOS,
                // or a retail Indian 13R. Software APIs cannot definitively distinguish internal PCB SKUs.
                d.equals("OP5D3BL1", ignoreCase = true) && p.contains("CPH2691", ignoreCase = true) -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Cross-Flash / Converted ROM Ambiguity",
                            observation = "Board 'OP5D3BL1' with Indian regional firmware 'CPH2691IN'",
                            implication = "Physical device may be a Chinese OnePlus Ace 5 with converted 13R firmware, or a retail 13R unit. Software APIs cannot distinguish physical PCB without hardware inspection."
                        )
                    )
                    physicalId = "Unconfirmed (Ace 5 China / 13R Global-IN Platform)"
                    confidence = PhysicalIdentityConfidence.UNCONFIRMED
                }
                m.contains("PKG110", ignoreCase = true) -> {
                    evidence.add(
                        DeviceIdentityEvidence(
                            factor = "Native China Model Suffix",
                            observation = "Model '$m' explicitly specifies Chinese Ace 5 SKU",
                            implication = "Probable Chinese Ace 5 physical hardware"
                        )
                    )
                    physicalId = "OnePlus Ace 5 (China)"
                    confidence = PhysicalIdentityConfidence.PROBABLE
                }
                m.contains("CPH2645", ignoreCase = true) -> {
                    physicalId = "OnePlus 13R (Global)"
                    confidence = PhysicalIdentityConfidence.PROBABLE
                }
                else -> {
                    physicalId = "$brand $model"
                    confidence = PhysicalIdentityConfidence.UNCONFIRMED
                }
            }

            return DeviceInfo(
                reportedManufacturer = manufacturer,
                reportedModel = model,
                reportedDevice = device,
                reportedProduct = product,
                reportedBrand = brand,
                reportedHardware = hardware,
                androidVersion = androidVersion,
                apiLevel = apiLevel,
                buildFingerprint = fingerprint,
                displayBuildId = display,
                firmwareIdentity = fwIdentity,
                probableHardwareFamily = hwFamily,
                physicalHardwareIdentity = physicalId,
                physicalIdentityConfidence = confidence,
                identityEvidence = evidence
            )
        }

        fun current(): DeviceInfo = analyze()
    }
}
