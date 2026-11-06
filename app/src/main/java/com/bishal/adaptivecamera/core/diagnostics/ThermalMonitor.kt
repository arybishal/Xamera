package com.bishal.adaptivecamera.core.diagnostics

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android hardware thermal status categories.
 */
enum class ThermalStatus(val severity: Int, val label: String) {
    NONE(PowerManager.THERMAL_STATUS_NONE, "Normal"),
    LIGHT(PowerManager.THERMAL_STATUS_LIGHT, "Light Warmth"),
    MODERATE(PowerManager.THERMAL_STATUS_MODERATE, "Moderate"),
    SEVERE(PowerManager.THERMAL_STATUS_SEVERE, "Severe Throttle"),
    CRITICAL(PowerManager.THERMAL_STATUS_CRITICAL, "Critical Temperature"),
    EMERGENCY(PowerManager.THERMAL_STATUS_EMERGENCY, "Emergency Shutdown Imminent"),
    SHUTDOWN(PowerManager.THERMAL_STATUS_SHUTDOWN, "Shutdown");

    val isThrottling: Boolean
        get() = severity >= PowerManager.THERMAL_STATUS_SEVERE

    companion object {
        fun fromSeverity(severity: Int): ThermalStatus {
            return entries.firstOrNull { it.severity == severity } ?: NONE
        }
    }
}

/**
 * Active thermal mitigation policy governing runtime workload adaptation.
 */
data class ThermalMitigationPolicy(
    val thermalStatus: ThermalStatus = ThermalStatus.NONE,
    val isThrottlingActive: Boolean = false,
    val disableAnalyticalOverlays: Boolean = false,
    val reduceGpuEffectsOnPreview: Boolean = false,
    val maxRecommendedStackingFrames: Int = 7,
    val advisoryNotice: String? = null
)

/**
 * Hardware Thermal Monitor (Phase 15 - FEAT-THRM-01).
 * Listens to OS thermal status changes via [PowerManager], computes adaptive mitigation
 * policies, and notifies camera and GPU pipelines to protect device thermals and battery.
 */
class ThermalMonitor(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _currentStatus = MutableStateFlow(ThermalStatus.NONE)
    val currentStatus: StateFlow<ThermalStatus> = _currentStatus.asStateFlow()

    private val _mitigationPolicy = MutableStateFlow(ThermalMitigationPolicy())
    val mitigationPolicy: StateFlow<ThermalMitigationPolicy> = _mitigationPolicy.asStateFlow()

    private var thermalListener: Any? = null

    init {
        initThermalMonitoring()
    }

    private fun initThermalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                val initialSeverity = powerManager.currentThermalStatus
                updateThermalSeverity(initialSeverity)

                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    scope.launch {
                        updateThermalSeverity(status)
                    }
                }
                powerManager.addThermalStatusListener(listener)
                thermalListener = listener
                Log.i(TAG, "ThermalStatusChangedListener registered successfully (initial: ${_currentStatus.value})")
            } catch (e: Exception) {
                Log.w(TAG, "Unable to register thermal status listener: ${e.message}")
            }
        }
    }

    fun updateThermalSeverity(severity: Int) {
        val status = ThermalStatus.fromSeverity(severity)
        _currentStatus.value = status

        val policy = when (status) {
            ThermalStatus.NONE -> ThermalMitigationPolicy(
                thermalStatus = status,
                isThrottlingActive = false,
                disableAnalyticalOverlays = false,
                reduceGpuEffectsOnPreview = false,
                maxRecommendedStackingFrames = 7,
                advisoryNotice = null
            )
            ThermalStatus.LIGHT -> ThermalMitigationPolicy(
                thermalStatus = status,
                isThrottlingActive = false,
                disableAnalyticalOverlays = false,
                reduceGpuEffectsOnPreview = false,
                maxRecommendedStackingFrames = 7,
                advisoryNotice = null
            )
            ThermalStatus.MODERATE -> ThermalMitigationPolicy(
                thermalStatus = status,
                isThrottlingActive = false,
                disableAnalyticalOverlays = false,
                reduceGpuEffectsOnPreview = false,
                maxRecommendedStackingFrames = 5,
                advisoryNotice = "Device warming up. Normal operations continuing."
            )
            ThermalStatus.SEVERE -> ThermalMitigationPolicy(
                thermalStatus = status,
                isThrottlingActive = true,
                disableAnalyticalOverlays = true,
                reduceGpuEffectsOnPreview = false,
                maxRecommendedStackingFrames = 3,
                advisoryNotice = "Thermal Throttling Active: Pausing analytical overlays to prevent heat build-up."
            )
            ThermalStatus.CRITICAL, ThermalStatus.EMERGENCY, ThermalStatus.SHUTDOWN -> ThermalMitigationPolicy(
                thermalStatus = status,
                isThrottlingActive = true,
                disableAnalyticalOverlays = true,
                reduceGpuEffectsOnPreview = true,
                maxRecommendedStackingFrames = 1,
                advisoryNotice = "High Temperature Alert: Limiting GPU effects and burst captures to protect camera sensor."
            )
        }

        _mitigationPolicy.value = policy
        Log.d(TAG, "Thermal mitigation policy updated: status=$status, throttling=${policy.isThrottlingActive}")
    }

    /**
     * Simulates a thermal status update for deterministic testing and validation.
     */
    fun simulateThermalStatus(status: ThermalStatus) {
        updateThermalSeverity(status.severity)
    }

    /**
     * Unregisters the thermal listener upon lifecycle termination.
     */
    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener?.let {
                try {
                    powerManager.removeThermalStatusListener(it as PowerManager.OnThermalStatusChangedListener)
                    thermalListener = null
                    Log.i(TAG, "ThermalStatusChangedListener unregistered")
                } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering thermal listener: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "ThermalMonitor"
    }
}
