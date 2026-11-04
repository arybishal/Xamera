package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build

data class DynamicRangeProfileDetail(
    val profileName: String,
    val bitDepth: Int,
    val isHdr: Boolean,
    val description: String
)

data class DynamicRangeInfo(
    val status: CapabilityStatus,
    val supportedProfiles: List<DynamicRangeProfileDetail> = emptyList(),
    val recommendedHdrProfile: String? = null,
    val notes: String = ""
)

object DynamicRangeAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): DynamicRangeInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return DynamicRangeInfo(
                status = CapabilityStatus.UNAVAILABLE,
                notes = "Dynamic range profiles require Android 13 (API 33) or higher"
            )
        }

        val drProfiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
        if (drProfiles == null) {
            return DynamicRangeInfo(
                status = CapabilityStatus.UNSUPPORTED,
                notes = "Hardware does not advertise DynamicRangeProfiles"
            )
        }

        val supported = drProfiles.supportedProfiles
        val details = mutableListOf<DynamicRangeProfileDetail>()

        // Standard SDR
        if (supported.contains(DynamicRangeProfiles.STANDARD)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "SDR (Standard)",
                    bitDepth = 8,
                    isHdr = false,
                    description = "Standard dynamic range 8-bit output (Rec.709 / sRGB)"
                )
            )
        }

        // HLG10
        if (supported.contains(DynamicRangeProfiles.HLG10)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "HLG10",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit Hybrid Log-Gamma (Rec.2020) broadcast HDR standard"
                )
            )
        }

        // HDR10
        if (supported.contains(DynamicRangeProfiles.HDR10)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "HDR10",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit SMPTE ST 2084 (PQ) static metadata HDR"
                )
            )
        }

        // HDR10+
        if (supported.contains(DynamicRangeProfiles.HDR10_PLUS)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "HDR10+",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit HDR10+ dynamic frame-by-frame metadata"
                )
            )
        }

        // Dolby Vision variants
        if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "Dolby Vision 10-bit OEM",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit Dolby Vision OEM tuned profile"
                )
            )
        }
        if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "Dolby Vision 10-bit OEM PO",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit Dolby Vision OEM Prioritized Output"
                )
            )
        }
        if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "Dolby Vision 10-bit Ref",
                    bitDepth = 10,
                    isHdr = true,
                    description = "10-bit Dolby Vision Reference profile"
                )
            )
        }
        if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM)) {
            details.add(
                DynamicRangeProfileDetail(
                    profileName = "Dolby Vision 8-bit OEM",
                    bitDepth = 8,
                    isHdr = true,
                    description = "8-bit Dolby Vision OEM profile"
                )
            )
        }

        val hasHdr = details.any { it.isHdr }
        val status = if (hasHdr) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        val recommended = when {
            details.any { it.profileName == "HLG10" } -> "HLG10"
            details.any { it.profileName == "HDR10+" } -> "HDR10+"
            details.any { it.profileName == "HDR10" } -> "HDR10"
            details.any { it.profileName.contains("Dolby Vision") } -> details.first { it.profileName.contains("Dolby Vision") }.profileName
            else -> null
        }

        return DynamicRangeInfo(
            status = status,
            supportedProfiles = details,
            recommendedHdrProfile = recommended,
            notes = if (hasHdr) "Device supports 10-bit HDR dynamic range output" else "Standard 8-bit dynamic range only"
        )
    }
}
