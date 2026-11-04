package com.bishal.adaptivecamera.camera.capability

enum class CapabilityStatus {
    SUPPORTED,
    CONFIGURATION_VALIDATED,
    CAPTURE_VERIFIED,
    UNSUPPORTED,
    UNAVAILABLE,
    NOT_VERIFIED,
    TEMPORARILY_UNAVAILABLE,
    ERROR;

    val isUsable: Boolean
        get() = this == SUPPORTED || this == CONFIGURATION_VALIDATED || this == CAPTURE_VERIFIED
}
