package com.bishal.adaptivecamera.camera.capability

enum class CapabilityStatus {
    SUPPORTED,
    UNSUPPORTED,
    UNAVAILABLE,
    NOT_VERIFIED,
    TEMPORARILY_UNAVAILABLE;

    val isUsable: Boolean
        get() = this == SUPPORTED
}
