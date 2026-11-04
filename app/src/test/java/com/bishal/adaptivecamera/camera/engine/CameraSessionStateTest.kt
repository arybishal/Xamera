package com.bishal.adaptivecamera.camera.engine

import com.bishal.adaptivecamera.camera.session.CameraSessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSessionStateTest {

    @Test
    fun testOperationalStates() {
        assertTrue(CameraSessionState.READY.isOperating)
        assertTrue(CameraSessionState.CAPTURING.isOperating)
        assertTrue(CameraSessionState.RECORDING.isOperating)

        assertFalse(CameraSessionState.CLOSED.isOperating)
        assertFalse(CameraSessionState.OPENING.isOperating)
        assertFalse(CameraSessionState.SWITCHING.isOperating)
        assertFalse(CameraSessionState.ERROR.isOperating)
    }

    @Test
    fun testCapturePhotoGuards() {
        // Can only initiate photo capture when camera is in READY state
        assertTrue(CameraSessionState.READY.canCapturePhoto)
        assertFalse(CameraSessionState.CAPTURING.canCapturePhoto)
        assertFalse(CameraSessionState.RECORDING.canCapturePhoto)
        assertFalse(CameraSessionState.OPENING.canCapturePhoto)
        assertFalse(CameraSessionState.CLOSED.canCapturePhoto)
        assertFalse(CameraSessionState.SWITCHING.canCapturePhoto)
        assertFalse(CameraSessionState.ERROR.canCapturePhoto)
    }

    @Test
    fun testRecordingGuards() {
        assertTrue(CameraSessionState.READY.canStartRecording)
        assertFalse(CameraSessionState.RECORDING.canStartRecording)
        assertFalse(CameraSessionState.CAPTURING.canStartRecording)

        assertTrue(CameraSessionState.RECORDING.canStopRecording)
        assertFalse(CameraSessionState.READY.canStopRecording)
    }

    @Test
    fun testCameraSwitchGuards() {
        // Cannot switch cameras while capturing or recording
        assertFalse(CameraSessionState.CAPTURING.canSwitchCamera)
        assertFalse(CameraSessionState.RECORDING.canSwitchCamera)
        assertFalse(CameraSessionState.OPENING.canSwitchCamera)

        assertTrue(CameraSessionState.READY.canSwitchCamera)
        assertTrue(CameraSessionState.ERROR.canSwitchCamera)
    }
}
