package com.bishal.adaptivecamera.camera.lens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Log

/**
 * Discovers and coordinates direct physical sub-camera access bypassing
 * the logical camera HAL compositor where supported (FEAT-FOC-06).
 */
class DirectLensSelector(private val context: Context) {

    private val cameraManager: CameraManager? = try {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    } catch (e: Exception) {
        null
    }

    /**
     * Discovers all available physical and logical back camera sensors.
     * On OnePlus 13R / Ace 5:
     * - Logical Camera 0 (Sony IMX890 50MP + 8MP Ultra-Wide compositor)
     * - Physical Sub-Camera 2 (Sony IMX890 50MP Wide Main sensor, ~5.59mm)
     * - Physical Sub-Camera 3 (8MP Ultra-Wide sensor, ~1.68mm)
     */
    fun discoverAvailableLenses(): List<DirectPhysicalLens> {
        val manager = cameraManager ?: return emptyList()
        val result = mutableListOf<DirectPhysicalLens>()

        try {
            val cameraIds = manager.cameraIdList
            val allPhysicalIds = mutableSetOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (id in cameraIds) {
                    try {
                        val chars = manager.getCameraCharacteristics(id)
                        allPhysicalIds.addAll(chars.physicalCameraIds)
                    } catch (_: Exception) {}
                }
            }

            for (id in cameraIds) {
                // Skip standalone physical sub-cameras from being added as pseudo-logical cameras
                if (id in allPhysicalIds) continue

                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing != CameraMetadata.LENS_FACING_BACK) continue

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(0f)
                val focal = focalLengths.firstOrNull() ?: 0f

                // Add logical back camera
                result.add(
                    DirectPhysicalLens(
                        cameraId = id,
                        type = PhysicalLensType.LOGICAL_BACK,
                        label = "Auto ($id)",
                        focalLengthMm = focal,
                        sensorSize = "Logical Multi-Camera",
                        isPhysicalSubCamera = false
                    )
                )

                // Check for physical sub-cameras on Android 9+ (API 28+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val physicalIds = chars.physicalCameraIds
                    for (pId in physicalIds) {
                        try {
                            val pChars = manager.getCameraCharacteristics(pId)
                            val pFocals = pChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(0f)
                            val pFocal = pFocals.firstOrNull() ?: 0f

                            val lensType = when {
                                pFocal in 0.1f..3.0f -> PhysicalLensType.ULTRA_WIDE
                                pFocal > 10.0f -> PhysicalLensType.TELEPHOTO
                                else -> PhysicalLensType.WIDE_MAIN
                            }

                            val label = when (lensType) {
                                PhysicalLensType.ULTRA_WIDE -> "0.6x UW (${String.format("%.1f", pFocal)}mm)"
                                PhysicalLensType.WIDE_MAIN -> "1x MAIN (${String.format("%.1f", pFocal)}mm)"
                                PhysicalLensType.TELEPHOTO -> "3x TELE (${String.format("%.1f", pFocal)}mm)"
                                else -> "Sensor #$pId"
                            }

                            result.add(
                                DirectPhysicalLens(
                                    cameraId = pId,
                                    type = lensType,
                                    label = label,
                                    focalLengthMm = pFocal,
                                    sensorSize = "Physical Sub-Sensor",
                                    isPhysicalSubCamera = true
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed reading characteristics for physical sub-camera $pId", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enumerating camera lenses", e)
        }

        return result
    }

    companion object {
        private const val TAG = "DirectLensSelector"
    }
}
