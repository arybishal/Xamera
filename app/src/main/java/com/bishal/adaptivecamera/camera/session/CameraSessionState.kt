package com.bishal.adaptivecamera.camera.session

enum class CameraSessionState {
    CLOSED,
    OPENING,
    READY,
    CAPTURING,
    RECORDING,
    SWITCHING,
    ERROR;

    val isOperating: Boolean
        get() = this == READY || this == CAPTURING || this == RECORDING

    val canCapturePhoto: Boolean
        get() = this == READY

    val canStartRecording: Boolean
        get() = this == READY

    val canStopRecording: Boolean
        get() = this == RECORDING

    val canSwitchCamera: Boolean
        get() = this == READY || this == ERROR
}
