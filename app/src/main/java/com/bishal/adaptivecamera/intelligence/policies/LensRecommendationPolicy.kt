package com.bishal.adaptivecamera.intelligence.policies

import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory

data class LensRecommendation(
    val recommendedCameraId: String?,
    val recommendedLensDescription: String,
    val reason: String,
    val requiresLensSwitch: Boolean
)

object LensRecommendationPolicy {

    fun recommendLens(
        activeCameraId: String,
        profiles: List<CameraCapabilityProfile>,
        sceneCategory: SceneCategory,
        currentZoomRatio: Float
    ): LensRecommendation {
        if (profiles.isEmpty()) {
            return LensRecommendation(
                recommendedCameraId = activeCameraId,
                recommendedLensDescription = "Default Lens",
                reason = "Hardware sensor inventory unavailable",
                requiresLensSwitch = false
            )
        }

        // Find primary back lens, ultra-wide lens, and front selfie lens
        val backProfiles = profiles.filter { it.isFacingBack }
        val ultraWideProfile = backProfiles.firstOrNull { profile ->
            profile.lensInfo.focalLengths.any { it < 2.5f } ||
                    profile.focalLengths.any { it < 2.5f } ||
                    profile.cameraId == "3"
        }
        val mainWideProfile = backProfiles.firstOrNull { profile ->
            profile.isLogicalCamera || profile.cameraId == "0" || profile.cameraId == "2"
        }

        when (sceneCategory) {
            SceneCategory.LANDSCAPE -> {
                // If zoom is wide or scene is expansive landscape, ultra-wide provides superior field of view
                if (currentZoomRatio <= 0.8f && ultraWideProfile != null && ultraWideProfile.cameraId != activeCameraId) {
                    return LensRecommendation(
                        recommendedCameraId = ultraWideProfile.cameraId,
                        recommendedLensDescription = "Ultra-Wide Lens (0.6x)",
                        reason = "Ultra-wide 1.68mm focal length recommended for wide landscape framing",
                        requiresLensSwitch = true
                    )
                }
            }

            SceneCategory.NIGHT -> {
                // Low light benefits from the main sensor's larger physical area (8.19 x 6.14 mm) and f/1.88 aperture
                if (mainWideProfile != null && activeCameraId != mainWideProfile.cameraId) {
                    return LensRecommendation(
                        recommendedCameraId = mainWideProfile.cameraId,
                        recommendedLensDescription = "Main Wide Sensor (f/1.88)",
                        reason = "Main high-aperture sensor captures 250% more light than auxiliary lenses",
                        requiresLensSwitch = true
                    )
                }
            }

            else -> Unit
        }

        // By default, current active lens is suitable
        val currentDesc = if (activeCameraId == ultraWideProfile?.cameraId) {
            "Ultra-Wide (0.6x)"
        } else if (activeCameraId == "1") {
            "Front Camera"
        } else {
            "Main Wide Sensor (1x)"
        }

        return LensRecommendation(
            recommendedCameraId = activeCameraId,
            recommendedLensDescription = currentDesc,
            reason = "Current lens optical characteristics match the scene",
            requiresLensSwitch = false
        )
    }
}
