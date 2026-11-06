package com.bishal.adaptivecamera.core.display

import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Window

/**
 * Diagnostic and configuration telemetry for display refresh rates.
 */
data class DisplayTelemetry(
    val currentModeId: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val refreshRateHz: Float = 60f,
    val supportedRefreshRates: List<Float> = emptyList(),
    val isHighRefreshRateActive: Boolean = false
)

/**
 * Manages device display refresh rate optimization to ensure smooth 120 Hz UI rendering
 * across viewfinders, pro dials, live meters, and animations.
 */
object DisplayRefreshRateManager {
    private const val TAG = "DisplayRefreshRate"
    private const val TARGET_REFRESH_RATE_HZ = 120.0f

    /**
     * Inspects supported display modes and configures the window to request optimal
     * 120 Hz refresh rate if supported by the display panel and OS.
     */
    fun applyOptimalRefreshRate(window: Window): DisplayTelemetry {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.context.display
            } else {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay
            } ?: return DisplayTelemetry()

            val modes = display.supportedModes ?: emptyArray()
            val rates = modes.map { it.refreshRate }.distinct().sorted()

            // Find high-refresh-rate mode (closest to 120 Hz, or highest available >= 90 Hz)
            val preferredMode = modes
                .filter { it.refreshRate >= 118.0f && it.refreshRate <= 145.0f }
                .maxByOrNull { it.physicalWidth * it.physicalHeight }
                ?: modes.maxByOrNull { it.refreshRate }

            if (preferredMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = preferredMode.modeId
                params.preferredRefreshRate = preferredMode.refreshRate
                window.attributes = params

                Log.i(
                    TAG,
                    "Configured display: modeId=${preferredMode.modeId}, resolution=${preferredMode.physicalWidth}x${preferredMode.physicalHeight}, rate=${preferredMode.refreshRate} Hz (available: $rates)"
                )

                return DisplayTelemetry(
                    currentModeId = preferredMode.modeId,
                    width = preferredMode.physicalWidth,
                    height = preferredMode.physicalHeight,
                    refreshRateHz = preferredMode.refreshRate,
                    supportedRefreshRates = rates,
                    isHighRefreshRateActive = preferredMode.refreshRate >= 118.0f
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply optimal refresh rate: ${e.message}")
        }
        return DisplayTelemetry()
    }

    /**
     * Retrieves current active display telemetry.
     */
    fun getTelemetry(window: Window): DisplayTelemetry {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.context.display
            } else {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay
            } ?: return DisplayTelemetry()

            val currentMode = display.mode
            val rates = display.supportedModes?.map { it.refreshRate }?.distinct()?.sorted() ?: emptyList()

            return DisplayTelemetry(
                currentModeId = currentMode.modeId,
                width = currentMode.physicalWidth,
                height = currentMode.physicalHeight,
                refreshRateHz = currentMode.refreshRate,
                supportedRefreshRates = rates,
                isHighRefreshRateActive = currentMode.refreshRate >= 118.0f
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read display telemetry: ${e.message}")
            return DisplayTelemetry()
        }
    }
}
