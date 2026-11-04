package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build

enum class CameraRelationship {
    LOGICAL,
    INDEPENDENT,
    PHYSICAL_REFERENCE,
    EXTERNAL,
    AUXILIARY_OR_UNKNOWN,
    NOT_VERIFIED
}

data class CameraRelationshipInfo(
    val relationship: CameraRelationship,
    val logicalParentIds: List<String> = emptyList(),
    val physicalSubCameraIds: List<String> = emptyList(),
    val isPhysicalStreamQuerySupported: Boolean = false,
    val description: String = ""
)

object CameraRelationshipAnalyzer {

    fun analyze(
        cameraIds: List<String>,
        characteristicsProvider: (String) -> CameraCharacteristics?
    ): Map<String, CameraRelationshipInfo> {
        val physicalToLogicalParents = mutableMapOf<String, MutableList<String>>()
        val logicalToPhysicals = mutableMapOf<String, List<String>>()
        val characteristicsMap = mutableMapOf<String, CameraCharacteristics>()

        for (id in cameraIds) {
            val chars = characteristicsProvider(id) ?: continue
            characteristicsMap[id] = chars

            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
            val isLogical = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)

            if (isLogical && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val subIds = chars.physicalCameraIds.toList()
                    logicalToPhysicals[id] = subIds
                    for (subId in subIds) {
                        physicalToLogicalParents.getOrPut(subId) { mutableListOf() }.add(id)
                    }
                } catch (_: Exception) {
                    logicalToPhysicals[id] = emptyList()
                }
            }
        }

        val result = mutableMapOf<String, CameraRelationshipInfo>()

        for (id in cameraIds) {
            val chars = characteristicsMap[id]
            if (chars == null) {
                result[id] = CameraRelationshipInfo(
                    relationship = CameraRelationship.NOT_VERIFIED,
                    description = "Characteristics unavailable"
                )
                continue
            }

            val rawFacing = chars.get(CameraCharacteristics.LENS_FACING) ?: -1
            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
            val isLogical = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
            val subCameraIds = logicalToPhysicals[id] ?: emptyList()
            val parentIds = physicalToLogicalParents[id] ?: emptyList()

            val relationship: CameraRelationship
            val desc: String

            when {
                rawFacing == CameraMetadata.LENS_FACING_EXTERNAL -> {
                    relationship = CameraRelationship.EXTERNAL
                    desc = "External / USB camera stream"
                }
                isLogical -> {
                    relationship = CameraRelationship.LOGICAL
                    desc = "Logical multi-camera composing physical sensors [${subCameraIds.joinToString(", ")}]"
                }
                parentIds.isNotEmpty() -> {
                    relationship = CameraRelationship.PHYSICAL_REFERENCE
                    desc = "Physical sub-sensor referenced by logical parent camera(s) [${parentIds.joinToString(", ")}]"
                }
                rawFacing == CameraMetadata.LENS_FACING_BACK || rawFacing == CameraMetadata.LENS_FACING_FRONT -> {
                    relationship = CameraRelationship.INDEPENDENT
                    desc = "Independent physical camera unit (Standalone)"
                }
                else -> {
                    relationship = CameraRelationship.AUXILIARY_OR_UNKNOWN
                    desc = "Auxiliary or vendor-specific camera stream"
                }
            }

            val physicalStreamQuerySupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && subCameraIds.isNotEmpty()) {
                true
            } else {
                false
            }

            result[id] = CameraRelationshipInfo(
                relationship = relationship,
                logicalParentIds = parentIds,
                physicalSubCameraIds = subCameraIds,
                isPhysicalStreamQuerySupported = physicalStreamQuerySupported,
                description = desc
            )
        }

        return result
    }
}
