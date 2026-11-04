package com.bishal.adaptivecamera.camera.capability

data class HardwareValidationSummary(
    val advertisedFeatures: Map<String, CapabilityStatus> = emptyMap(),
    val apiAccessibleFeatures: Map<String, CapabilityStatus> = emptyMap(),
    val validatedConfigurations: Map<String, CapabilityStatus> = emptyMap(),
    val captureVerifiedFeatures: Map<String, CapabilityStatus> = emptyMap()
) {
    val totalAdvertised: Int
        get() = advertisedFeatures.size

    val usableAdvertisedCount: Int
        get() = advertisedFeatures.count { it.value.isUsable }

    val validatedCount: Int
        get() = validatedConfigurations.count { it.value.isUsable }

    val verifiedCount: Int
        get() = captureVerifiedFeatures.count { it.value.isUsable }
}
