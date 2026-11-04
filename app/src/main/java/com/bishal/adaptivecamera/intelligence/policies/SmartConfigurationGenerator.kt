package com.bishal.adaptivecamera.intelligence.policies

import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetrics
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory
import com.bishal.adaptivecamera.intelligence.scene.SceneClassificationResult
import kotlin.math.max
import kotlin.math.min

data class SmartConfigSuggestion(
    val scene: SceneClassificationResult,
    val recommendedIso: Int?,
    val recommendedShutterSpeedNs: Long?,
    val recommendedEvIndex: Int,
    val isHdrRecommended: Boolean,
    val isNightModeRecommended: Boolean,
    val isFillFlashRecommended: Boolean,
    val actionLabel: String,
    val summaryRationale: String
)

object SmartConfigurationGenerator {

    /**
     * Synthesizes frame metrics, classified scene, and hardware profile to produce
     * photographic parameter recommendations.
     */
    fun generateSuggestion(
        sceneResult: SceneClassificationResult,
        metrics: FrameMetrics,
        profile: CameraCapabilityProfile? = null
    ): SmartConfigSuggestion {
        val isoMin = profile?.isoRange?.lower ?: 100
        val isoMax = profile?.isoRange?.upper ?: 3200

        var recIso: Int? = null
        var recShutterNs: Long? = null
        var recEv = 0
        var hdr = false
        var night = false
        var fillFlash = false
        var actionLabel = "Scene Analyzed"
        var rationale = sceneResult.explanation

        when (sceneResult.primaryCategory) {
            SceneCategory.NIGHT -> {
                night = true
                recEv = -1 // Slight underexposure to protect highlights in dark scenes
                recIso = min(isoMax, max(isoMin, 1600))
                recShutterNs = 100_000_000L // 1/10s target for multi-frame
                actionLabel = "Night Mode Active"
                rationale = "Long exposure & multi-frame noise reduction applied for low-light."
            }

            SceneCategory.BACKLIT -> {
                hdr = true
                recEv = +1 // +0.33 to +0.67 EV boost to lift shadowed foreground
                recIso = isoMin // Keep base ISO for cleanest shadow extraction
                actionLabel = "HDR Shadow Recovery"
                rationale = "High contrast detected: HDR exposure bracketing enabled to recover shadowed subject."
            }

            SceneCategory.ACTION_MOVEMENT -> {
                // High shutter priority (1/500s = 2,000,000 ns to 1/1000s = 1,000,000 ns)
                recShutterNs = 1_000_000L
                recIso = min(isoMax, max(isoMin, 800))
                actionLabel = "Action Shutter Priority"
                rationale = "Fast shutter target (1/1000s) selected to freeze high-speed motion."
            }

            SceneCategory.PORTRAIT -> {
                recEv = 0
                recIso = isoMin
                if (metrics.isHighContrastOrBacklit) {
                    fillFlash = true
                    hdr = true
                    actionLabel = "Portrait HDR Fill"
                    rationale = "Gentle HDR fill recommended to balance portrait face exposure against bright background."
                } else {
                    actionLabel = "Portrait Tonal Softening"
                    rationale = "Natural skin-tone rendering optimized for portrait focal plane."
                }
            }

            SceneCategory.LANDSCAPE -> {
                recEv = 0
                recIso = isoMin // Cleanest detail at lowest native ISO
                hdr = metrics.estimatedDynamicRangeStops > 7.5f
                actionLabel = if (hdr) "Landscape Wide-HDR" else "Landscape Sharpness"
                rationale = "Base ISO and wide dynamic range selected for crisp landscape details."
            }

            SceneCategory.MACRO -> {
                recEv = 0
                recIso = isoMin
                actionLabel = "Macro Focus Precision"
                rationale = "Close-up range detected: shallow depth-of-field precision lock active."
            }

            SceneCategory.STANDARD -> {
                recEv = 0
                actionLabel = "Smart Auto Balanced"
                rationale = "Scene is well-balanced within native dynamic range."
            }
        }

        return SmartConfigSuggestion(
            scene = sceneResult,
            recommendedIso = recIso,
            recommendedShutterSpeedNs = recShutterNs,
            recommendedEvIndex = recEv,
            isHdrRecommended = hdr,
            isNightModeRecommended = night,
            isFillFlashRecommended = fillFlash,
            actionLabel = actionLabel,
            summaryRationale = rationale
        )
    }
}
