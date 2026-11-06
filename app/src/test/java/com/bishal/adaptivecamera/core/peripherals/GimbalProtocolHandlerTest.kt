package com.bishal.adaptivecamera.core.peripherals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GimbalProtocolHandlerTest {

    private val handler = GimbalProtocolHandler()

    @Test
    fun testDjiShutterPacket() {
        handler.activeBrand = GimbalBrand.DJI_OSMO_MOBILE
        // Header: 0x55, Len: 0x05, CmdSet: 0x01, CmdId: 0x01 (Shutter)
        val packet = byteArrayOf(0x55.toByte(), 0x05, 0x01, 0x01, 0x00)
        val action = handler.parsePacket(packet)

        assertEquals(GimbalAction.TriggerShutter, action)
    }

    @Test
    fun testDjiRecordingPacket() {
        handler.activeBrand = GimbalBrand.DJI_OSMO_MOBILE
        // Start recording
        val startPacket = byteArrayOf(0x55.toByte(), 0x05, 0x01, 0x02, 0x01)
        assertEquals(GimbalAction.StartRecording, handler.parsePacket(startPacket))

        // Stop recording
        val stopPacket = byteArrayOf(0x55.toByte(), 0x05, 0x01, 0x02, 0x00)
        assertEquals(GimbalAction.StopRecording, handler.parsePacket(stopPacket))
    }

    @Test
    fun testDjiZoomDeltaPacket() {
        handler.activeBrand = GimbalBrand.DJI_OSMO_MOBILE
        // Zoom positive delta
        val zoomPacket = byteArrayOf(0x55.toByte(), 0x05, 0x01, 0x03, 0x14) // +20
        val action = handler.parsePacket(zoomPacket)

        assertTrue(action is GimbalAction.ZoomDelta)
        assertEquals(0.2f, (action as GimbalAction.ZoomDelta).deltaRatio, 0.01f)
    }

    @Test
    fun testZhiyunShutterPacket() {
        handler.activeBrand = GimbalBrand.ZHIYUN_SMOOTH
        // Header: 0x24, Len: 0x03, KeyCode: 0x10 (Shutter)
        val packet = byteArrayOf(0x24.toByte(), 0x03, 0x10, 0x00)
        val action = handler.parsePacket(packet)

        assertEquals(GimbalAction.TriggerShutter, action)
    }

    @Test
    fun testZhiyunZoomPackets() {
        handler.activeBrand = GimbalBrand.ZHIYUN_SMOOTH
        val zoomInPacket = byteArrayOf(0x24.toByte(), 0x03, 0x20, 0x00)
        val actionIn = handler.parsePacket(zoomInPacket)
        assertTrue(actionIn is GimbalAction.ZoomDelta)
        assertEquals(0.1f, (actionIn as GimbalAction.ZoomDelta).deltaRatio, 0.01f)

        val zoomOutPacket = byteArrayOf(0x24.toByte(), 0x03, 0x21, 0x00)
        val actionOut = handler.parsePacket(zoomOutPacket)
        assertTrue(actionOut is GimbalAction.ZoomDelta)
        assertEquals(-0.1f, (actionOut as GimbalAction.ZoomDelta).deltaRatio, 0.01f)
    }
}
