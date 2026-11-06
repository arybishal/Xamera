package com.bishal.adaptivecamera.camera.controls

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

interface ZoomController {
    val zoomRatio: StateFlow<Float>
    val minZoomRatio: StateFlow<Float>
    val maxZoomRatio: StateFlow<Float>

    fun updateRange(minRatio: Float, maxRatio: Float)
    fun setZoomRatio(ratio: Float): Float
    fun applyScaleFactor(factor: Float): Float
    fun getAvailableSteps(): List<Float>
    fun isNearestStep(step: Float, tolerance: Float = 0.08f): Boolean
}

class ZoomControllerImpl(
    initialZoom: Float = 1.0f,
    initialMin: Float = 1.0f,
    initialMax: Float = 10.0f,
    private val onZoomRatioApplied: (Float) -> Unit
) : ZoomController {

    private val _zoomRatio = MutableStateFlow(initialZoom)
    override val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _minZoomRatio = MutableStateFlow(initialMin)
    override val minZoomRatio: StateFlow<Float> = _minZoomRatio.asStateFlow()

    private val _maxZoomRatio = MutableStateFlow(initialMax)
    override val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()

    override fun updateRange(minRatio: Float, maxRatio: Float) {
        val safeMin = minRatio.coerceAtLeast(0.1f)
        val safeMax = maxRatio.coerceAtLeast(safeMin)

        _minZoomRatio.value = safeMin
        _maxZoomRatio.value = safeMax

        // Ensure current zoom remains in bounds
        val current = _zoomRatio.value
        val clamped = current.coerceIn(safeMin, safeMax)
        if (clamped != current) {
            _zoomRatio.value = clamped
            onZoomRatioApplied(clamped)
        }
    }

    override fun setZoomRatio(ratio: Float): Float {
        val min = _minZoomRatio.value
        val max = _maxZoomRatio.value
        val clamped = ratio.coerceIn(min, max)

        if (abs(_zoomRatio.value - clamped) > 0.001f) {
            _zoomRatio.value = clamped
            onZoomRatioApplied(clamped)
        }
        return clamped
    }

    override fun applyScaleFactor(factor: Float): Float {
        if (factor <= 0f) return _zoomRatio.value
        val target = _zoomRatio.value * factor
        return setZoomRatio(target)
    }

    override fun getAvailableSteps(): List<Float> {
        val min = _minZoomRatio.value
        val max = _maxZoomRatio.value
        // Include 0.5x if hardware supports it (min <= 0.55f),
        // or 0.6x if min is ~0.6x (e.g. OnePlus 13R / Ace 5 physical hardware minimum)
        val candidateSteps = if (min <= 0.55f) {
            listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
        } else if (min <= 0.65f) {
            listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
        } else {
            listOf(1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
        }
        val valid = candidateSteps.filter { it in min..max }
        return if (valid.isEmpty()) listOf(min) else valid
    }

    override fun isNearestStep(step: Float, tolerance: Float): Boolean {
        return abs(_zoomRatio.value - step) <= tolerance
    }
}
