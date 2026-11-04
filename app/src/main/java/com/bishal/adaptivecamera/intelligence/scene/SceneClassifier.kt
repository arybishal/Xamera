package com.bishal.adaptivecamera.intelligence.scene

import com.bishal.adaptivecamera.intelligence.analysis.FrameMetrics
import kotlin.math.min

data class SceneClassificationResult(
    val primaryCategory: SceneCategory,
    val confidence: Float,
    val explanation: String,
    val secondaryCategory: SceneCategory? = null
)

object SceneClassifier {

    /**
     * Evaluates frame metrics and camera hardware state to classify the active scene.
     */
    fun classify(
        metrics: FrameMetrics,
        currentZoom: Float = 1.0f,
        focusDistanceDiopters: Float? = null,
        isFacingFront: Boolean = false
    ): SceneClassificationResult {
        // 1. MACRO EVALUATION:
        // Focus distance in diopters (d = 1/m). If d >= 6.67 diopters, distance is <= 15cm!
        if (focusDistanceDiopters != null && focusDistanceDiopters >= 6.67f && !isFacingFront) {
            val cm = (100.0f / focusDistanceDiopters).toInt()
            val conf = min(1.0f, 0.70f + (focusDistanceDiopters - 6.67f) * 0.05f)
            return SceneClassificationResult(
                primaryCategory = SceneCategory.MACRO,
                confidence = conf,
                explanation = "Macro close-up focus detected: subject distance approx ${cm}cm (${String.format("%.1f", focusDistanceDiopters)} diopters)"
            )
        }

        // 2. ACTION / MOVEMENT EVALUATION:
        if (metrics.isHighMotion) {
            val conf = min(1.0f, 0.65f + (metrics.motionDelta - 0.18f) * 1.5f)
            val secondary = if (metrics.isLowLight) SceneCategory.NIGHT else null
            return SceneClassificationResult(
                primaryCategory = SceneCategory.ACTION_MOVEMENT,
                confidence = conf,
                explanation = "Rapid motion detected: frame temporal delta is ${String.format("%.1f", metrics.motionDelta * 100)}%",
                secondaryCategory = secondary
            )
        }

        // 3. NIGHT / LOW-LIGHT EVALUATION:
        if (metrics.isLowLight) {
            val lumaPercent = (metrics.meanLuminance * 100).toInt()
            val shadowPercent = (metrics.shadowRatio * 100).toInt()
            val conf = min(1.0f, 0.75f + (0.25f - metrics.meanLuminance) * 1.2f)
            return SceneClassificationResult(
                primaryCategory = SceneCategory.NIGHT,
                confidence = conf,
                explanation = "Low ambient luminance (${lumaPercent}%) with extensive shadows (${shadowPercent}%)",
                secondaryCategory = if (metrics.highlightRatio > 0.10f) SceneCategory.BACKLIT else null
            )
        }

        // 4. BACKLIT / HIGH DYNAMIC RANGE EVALUATION:
        if (metrics.isHighContrastOrBacklit) {
            val drStops = String.format("%.1f", metrics.estimatedDynamicRangeStops)
            val conf = min(1.0f, 0.70f + (metrics.rmsContrast - 0.28f) * 1.5f)
            val secondary = if (isFacingFront) SceneCategory.PORTRAIT else null
            return SceneClassificationResult(
                primaryCategory = SceneCategory.BACKLIT,
                confidence = conf,
                explanation = "High contrast backlight detected: dynamic range ${drStops} stops with strong highlights and deep shadows",
                secondaryCategory = secondary
            )
        }

        // 5. PORTRAIT EVALUATION (Front camera or mid-distance zoom with high mid-tone dominance):
        if (isFacingFront) {
            return SceneClassificationResult(
                primaryCategory = SceneCategory.PORTRAIT,
                confidence = 0.88f,
                explanation = "Front camera selfie orientation detected"
            )
        } else if (metrics.midtoneRatio > 0.75f && currentZoom >= 1.5f && currentZoom <= 3.5f && metrics.motionDelta < 0.08f) {
            return SceneClassificationResult(
                primaryCategory = SceneCategory.PORTRAIT,
                confidence = 0.72f,
                explanation = "Focal mid-range framing with balanced midtone distribution (${(metrics.midtoneRatio * 100).toInt()}%)"
            )
        }

        // 6. LANDSCAPE EVALUATION:
        // Wide field-of-view (zoom <= 1.0x), stable camera (motion < 0.05), broad dynamic range (> 6 stops)
        if (currentZoom <= 1.0f && metrics.motionDelta < 0.06f && metrics.estimatedDynamicRangeStops >= 6.0f && metrics.meanLuminance in 0.30f..0.75f) {
            return SceneClassificationResult(
                primaryCategory = SceneCategory.LANDSCAPE,
                confidence = 0.80f,
                explanation = "Expansive composition with steady framing and broad tonal scale"
            )
        }

        // 7. STANDARD / BALANCED FALLBACK:
        return SceneClassificationResult(
            primaryCategory = SceneCategory.STANDARD,
            confidence = 0.90f,
            explanation = "Balanced ambient light and composition"
        )
    }
}
