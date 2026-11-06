package com.bishal.adaptivecamera.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class VirtualHorizonState(
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val isLevel: Boolean = false,
    val isAvailable: Boolean = true
)

interface VirtualHorizonSensor {
    val horizonState: StateFlow<VirtualHorizonState>
    fun startListening()
    fun stopListening()
}

object VirtualHorizonCalculator {
    const val LEVEL_TOLERANCE_DEGREES = 0.75f
    const val FILTER_ALPHA = 0.22f // Exponential moving average smoothing factor

    fun normalizeRoll(rawRoll: Float): Float = when {
        rawRoll in -45f..45f -> rawRoll // Portrait
        rawRoll > 45f && rawRoll <= 135f -> rawRoll - 90f // Landscape right
        rawRoll < -45f && rawRoll >= -135f -> rawRoll + 90f // Landscape left
        rawRoll > 135f -> rawRoll - 180f
        rawRoll < -135f -> rawRoll + 180f
        else -> rawRoll
    }

    fun applySmoothing(current: Float, target: Float, alpha: Float = FILTER_ALPHA): Float {
        return (current * (1f - alpha)) + (target * alpha)
    }

    fun isLevel(rollDegrees: Float, tolerance: Float = LEVEL_TOLERANCE_DEGREES): Boolean {
        return abs(rollDegrees) <= tolerance
    }
}

class VirtualHorizonSensorImpl(
    context: Context
) : VirtualHorizonSensor, SensorEventListener {

    companion object {
        private const val TAG = "VirtualHorizonSensor"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val fallbackAccelerometer: Sensor? = if (rotationVectorSensor == null) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    } else null

    private val _horizonState = MutableStateFlow(
        VirtualHorizonState(isAvailable = rotationVectorSensor != null || fallbackAccelerometer != null)
    )
    override val horizonState: StateFlow<VirtualHorizonState> = _horizonState.asStateFlow()

    private var isListening = false
    private var smoothedRoll = 0f
    private var smoothedPitch = 0f
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun startListening() {
        if (isListening || sensorManager == null) return
        val sensorToRegister = rotationVectorSensor ?: fallbackAccelerometer ?: return

        val registered = sensorManager.registerListener(
            this,
            sensorToRegister,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = registered
        if (!registered) {
            Log.w(TAG, "Failed to register sensor listener for horizon")
            _horizonState.update { it.copy(isAvailable = false) }
        }
    }

    override fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                // Azimuth: orientationAngles[0], Pitch: orientationAngles[1], Roll: orientationAngles[2]
                val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                processOrientations(rawRoll, rawPitch)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                val rawRoll = Math.toDegrees(atan2(-ax.toDouble(), ay.toDouble())).toFloat()
                val rawPitch = Math.toDegrees(atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))).toFloat()

                processOrientations(rawRoll, rawPitch)
            }
        }
    }

    private fun processOrientations(rawRoll: Float, rawPitch: Float) {
        val normalizedRoll = VirtualHorizonCalculator.normalizeRoll(rawRoll)

        // Apply low-pass exponential moving average filter
        smoothedRoll = VirtualHorizonCalculator.applySmoothing(smoothedRoll, normalizedRoll)
        smoothedPitch = VirtualHorizonCalculator.applySmoothing(smoothedPitch, rawPitch)

        val isLevel = VirtualHorizonCalculator.isLevel(smoothedRoll)

        _horizonState.update {
            it.copy(
                rollDegrees = smoothedRoll,
                pitchDegrees = smoothedPitch,
                isLevel = isLevel
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
