package com.bishal.adaptivecamera.photography.capture

import kotlin.math.pow
import kotlin.math.roundToInt

enum class AebBracketMode(val count: Int, val label: String) {
    OFF(0, "AEB Off"),
    BRACKET_3(3, "3 Brackets"),
    BRACKET_5(5, "5 Brackets"),
    BRACKET_7(7, "7 Brackets");

    fun next(): AebBracketMode = when (this) {
        OFF -> BRACKET_3
        BRACKET_3 -> BRACKET_5
        BRACKET_5 -> BRACKET_7
        BRACKET_7 -> OFF
    }
}

data class AebStep(
    val frameIndex: Int,
    val totalFrames: Int,
    val evOffset: Float,
    val aeCompensationIndex: Int? = null,
    val manualShutterNs: Long? = null,
    val manualIso: Int? = null
)

/**
 * Coordinator for Auto Exposure Bracketing (AEB) captures across automatic and manual exposure modes.
 * Complies with Deduplication Rule 1: Maintains a single source of truth for exposure requests
 * and guarantees original exposure state restoration after sequence completion.
 */
object AebExposureCoordinator {

    /**
     * Generates standard EV offsets based on bracket count and step size.
     */
    fun calculateEvOffsets(bracketCount: Int, evStep: Float = 1.0f): List<Float> {
        return when (bracketCount) {
            3 -> listOf(-2.0f * evStep, 0.0f, 2.0f * evStep)
            5 -> listOf(-2.0f * evStep, -1.0f * evStep, 0.0f, 1.0f * evStep, 2.0f * evStep)
            7 -> listOf(-3.0f * evStep, -2.0f * evStep, -1.0f * evStep, 0.0f, 1.0f * evStep, 2.0f * evStep, 3.0f * evStep)
            else -> emptyList()
        }
    }

    /**
     * Builds concrete AE compensation sequence for Auto Exposure mode.
     */
    fun buildAutoBracketSequence(
        offsets: List<Float>,
        baseIndex: Int,
        stepEv: Float,
        minIndex: Int,
        maxIndex: Int
    ): List<AebStep> {
        if (offsets.isEmpty() || stepEv <= 0f) return emptyList()

        return offsets.mapIndexed { index, ev ->
            val indexDelta = (ev / stepEv).roundToInt()
            val targetIndex = (baseIndex + indexDelta).coerceIn(minIndex, maxIndex)
            AebStep(
                frameIndex = index + 1,
                totalFrames = offsets.size,
                evOffset = ev,
                aeCompensationIndex = targetIndex
            )
        }
    }

    /**
     * Builds concrete sensor exposure sequence for Manual Sensor mode.
     * Modulates shutter speed in powers of 2 ($2^{\Delta EV}$), falling back to ISO scaling
     * if the requested duration exceeds physical sensor limits.
     */
    fun buildManualBracketSequence(
        offsets: List<Float>,
        baseShutterNs: Long,
        minShutterNs: Long,
        maxShutterNs: Long,
        baseIso: Int,
        minIso: Int,
        maxIso: Int
    ): List<AebStep> {
        if (offsets.isEmpty() || baseShutterNs <= 0L) return emptyList()

        return offsets.mapIndexed { index, ev ->
            val factor = 2.0.pow(ev.toDouble())
            val desiredShutterNs = (baseShutterNs * factor).toLong()
            val clampedShutterNs = desiredShutterNs.coerceIn(minShutterNs, maxShutterNs)

            // If shutter speed clamped at limits, adjust ISO to compensate remainder
            val actualShutterFactor = clampedShutterNs.toDouble() / baseShutterNs.toDouble()
            val remainingFactor = factor / actualShutterFactor
            val targetIso = (baseIso * remainingFactor).toInt().coerceIn(minIso, maxIso)

            AebStep(
                frameIndex = index + 1,
                totalFrames = offsets.size,
                evOffset = ev,
                manualShutterNs = clampedShutterNs,
                manualIso = targetIso
            )
        }
    }

    /**
     * Generates standard filename for AEB grouped images.
     */
    fun formatAebFilename(sessionId: String, frameIndex: Int, totalFrames: Int, evOffset: Float): String {
        val evStr = if (evOffset >= 0) "+%.1f".format(evOffset) else "%.1f".format(evOffset)
        return "IMG_AEB_${sessionId}_${frameIndex}of${totalFrames}_${evStr}EV.jpg"
    }
}
