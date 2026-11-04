package com.bishal.adaptivecamera.camera.capability

import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo

data class DeviceCapabilityReport(
    val deviceInfo: DeviceInfo,
    val cameraProfiles: List<CameraCapabilityProfile>,
    val scannedTimestamp: Long = System.currentTimeMillis(),
    val diagnosticWarnings: List<String> = emptyList()
) {
    fun toFormattedText(): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("       ADAPTIVE CAMERA HARDWARE CAPABILITY REPORT")
        sb.appendLine("==================================================")
        sb.appendLine("Generated At: $scannedTimestamp")
        sb.appendLine("Reported Model: ${deviceInfo.reportedModel}")
        sb.appendLine("Reported Device / Board: ${deviceInfo.reportedDevice}")
        sb.appendLine("Reported Product: ${deviceInfo.reportedProduct}")
        sb.appendLine("Reported Manufacturer: ${deviceInfo.reportedManufacturer} (${deviceInfo.reportedBrand})")
        sb.appendLine("Reported Hardware: ${deviceInfo.reportedHardware}")
        sb.appendLine("Android OS Version: ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
        sb.appendLine("Build ID: ${deviceInfo.displayBuildId}")
        sb.appendLine("Fingerprint: ${deviceInfo.buildFingerprint}")
        sb.appendLine()
        sb.appendLine("--- HARDWARE CLASSIFICATION AUDIT ---")
        sb.appendLine("Firmware Identity: ${deviceInfo.firmwareIdentity}")
        sb.appendLine("Hardware Family: ${deviceInfo.probableHardwareFamily}")
        sb.appendLine("Physical Hardware: ${deviceInfo.physicalHardwareIdentity}")
        sb.appendLine("Physical Identity Confidence: ${deviceInfo.physicalIdentityConfidence}")
        if (deviceInfo.identityEvidence.isNotEmpty()) {
            sb.appendLine("Identity Evidence Factors:")
            deviceInfo.identityEvidence.forEach { e ->
                sb.appendLine("  * [${e.factor}] ${e.observation} -> ${e.implication}")
            }
        }
        sb.appendLine()
        sb.appendLine("--- CAMERA SENSOR INVENTORY ---")
        sb.appendLine("Total Accessible Camera Devices: ${cameraProfiles.size}")
        val backCount = cameraProfiles.count { it.isFacingBack }
        val frontCount = cameraProfiles.count { it.isFacingFront }
        sb.appendLine("Back-Facing Cameras: $backCount | Front-Facing Cameras: $frontCount")
        val logicalCount = cameraProfiles.count { it.isLogicalCamera }
        val rawCount = cameraProfiles.count { it.supportsRaw }
        val manualCount = cameraProfiles.count { it.supportsManualSensor }
        sb.appendLine("Logical Multi-Cameras: $logicalCount | RAW Capable: $rawCount | Manual Sensor: $manualCount")
        sb.appendLine()

        if (diagnosticWarnings.isNotEmpty()) {
            sb.appendLine("--- DIAGNOSTIC WARNINGS & NOTICES ---")
            diagnosticWarnings.forEach { warning ->
                sb.appendLine("! $warning")
            }
            sb.appendLine()
        }

        cameraProfiles.forEachIndexed { index, cam ->
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("CAMERA #${index + 1} [ID: ${cam.cameraId} | ${cam.facingLabel} (raw: ${cam.rawFacing})]")
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("Hardware Level: ${cam.hardwareLevel.displayName} (raw: ${cam.rawHardwareLevel})")
            sb.appendLine("Logical Multi-Camera: ${cam.isLogicalCamera}")
            if (cam.physicalCameraIds.isNotEmpty()) {
                sb.appendLine("Physical Camera IDs: ${cam.physicalCameraIds.joinToString(", ")}")
            }
            sb.appendLine("Sensor Orientation: ${cam.sensorOrientation}°")
            sb.appendLine("Active Array Size: ${cam.sensorActiveArraySize}")
            if (cam.focalLengths.isNotEmpty()) {
                sb.appendLine("Focal Lengths: ${cam.focalLengths.joinToString(", ") { "${it}mm" }}")
            }
            sb.appendLine("Zoom Range: ${cam.minZoomRatio}x - ${cam.maxZoomRatio}x (Status: ${cam.zoomStatus})")

            sb.appendLine("Autofocus: ${cam.autoFocusStatus}")
            sb.appendLine("Manual Focus: ${cam.manualFocusStatus}")
            if (cam.minimumFocusDistance != null) {
                sb.appendLine("Min Focus Distance: ${cam.minimumFocusDistance} diopters")
            }
            sb.appendLine("Manual Sensor: ${cam.manualSensorStatus}")
            if (cam.isoRange != null) {
                sb.appendLine("ISO Range: ${cam.isoRange.lower} - ${cam.isoRange.upper}")
            }
            if (cam.exposureTimeRangeNs != null) {
                sb.appendLine("Exposure Time Range: ${cam.exposureTimeRangeDisplay}")
            }
            if (cam.exposureCompensationRange != null && cam.exposureCompensationStep != null) {
                val minEv = cam.exposureCompensationRange.lower * cam.exposureCompensationStep
                val maxEv = cam.exposureCompensationRange.upper * cam.exposureCompensationStep
                sb.appendLine("Exposure Comp: [${minEv}EV, ${maxEv}EV] step ${cam.exposureCompensationStep}EV")
            }
            sb.appendLine("Flash Unit: ${cam.flashStatus}")
            sb.appendLine("RAW Stream Support: ${cam.rawStatus}")
            if (cam.rawFormats.isNotEmpty()) {
                sb.appendLine("RAW Formats: ${cam.rawFormats.joinToString()}")
            }
            sb.appendLine("HDR Output Profile Support: ${cam.hdrStatus}")
            if (cam.dynamicRangeProfilesSupported.isNotEmpty()) {
                sb.appendLine("Dynamic Range Profiles: ${cam.dynamicRangeProfilesSupported.joinToString(", ")}")
            }

            sb.appendLine("Max Photo Size: ${cam.maxPhotoResolution ?: "None"}")
            val topPhotoSizes = cam.supportedPhotoSizes.take(5).joinToString(", ") { it.toString() }
            sb.appendLine("Top Photo Sizes: $topPhotoSizes")

            sb.appendLine("Max Video Size: ${cam.maxVideoResolution ?: "None"}")
            val topVideoSizes = cam.supportedVideoSizes.take(5).joinToString(", ") { it.toString() }
            sb.appendLine("Top Video Sizes: $topVideoSizes")

            val fpsRangesStr = cam.supportedFpsRanges.joinToString(", ") { it.toString() }
            sb.appendLine("Supported FPS Ranges: $fpsRangesStr")

            if (cam.diagnosticNotes.isNotEmpty()) {
                sb.appendLine("Diagnostic Notes:")
                cam.diagnosticNotes.forEach { note ->
                    sb.appendLine("  - $note")
                }
            }
            sb.appendLine()
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun toJson(): String {
        val escape = { s: String ->
            s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"timestamp\": $scannedTimestamp,\n")
        sb.append("  \"deviceInfo\": {\n")
        sb.append("    \"reportedModel\": \"${escape(deviceInfo.reportedModel)}\",\n")
        sb.append("    \"reportedDevice\": \"${escape(deviceInfo.reportedDevice)}\",\n")
        sb.append("    \"reportedProduct\": \"${escape(deviceInfo.reportedProduct)}\",\n")
        sb.append("    \"reportedManufacturer\": \"${escape(deviceInfo.reportedManufacturer)}\",\n")
        sb.append("    \"reportedBrand\": \"${escape(deviceInfo.reportedBrand)}\",\n")
        sb.append("    \"reportedHardware\": \"${escape(deviceInfo.reportedHardware)}\",\n")
        sb.append("    \"androidVersion\": \"${escape(deviceInfo.androidVersion)}\",\n")
        sb.append("    \"apiLevel\": ${deviceInfo.apiLevel},\n")
        sb.append("    \"buildFingerprint\": \"${escape(deviceInfo.buildFingerprint)}\",\n")
        sb.append("    \"firmwareIdentity\": \"${escape(deviceInfo.firmwareIdentity)}\",\n")
        sb.append("    \"probableHardwareFamily\": \"${escape(deviceInfo.probableHardwareFamily)}\",\n")
        sb.append("    \"physicalHardwareIdentity\": \"${escape(deviceInfo.physicalHardwareIdentity)}\",\n")
        sb.append("    \"physicalIdentityConfidence\": \"${deviceInfo.physicalIdentityConfidence}\"\n")
        sb.append("  },\n")
        sb.append("  \"diagnosticWarnings\": [")
        sb.append(diagnosticWarnings.joinToString(",") { "\"${escape(it)}\"" })
        sb.append("],\n")
        sb.append("  \"cameraProfiles\": [\n")
        cameraProfiles.forEachIndexed { i, c ->
            sb.append("    {\n")
            sb.append("      \"cameraId\": \"${escape(c.cameraId)}\",\n")
            sb.append("      \"rawFacing\": ${c.rawFacing},\n")
            sb.append("      \"facingLabel\": \"${c.facingLabel}\",\n")
            sb.append("      \"hardwareLevel\": \"${c.hardwareLevel.displayName}\",\n")
            sb.append("      \"rawHardwareLevel\": ${c.rawHardwareLevel},\n")
            sb.append("      \"isLogicalCamera\": ${c.isLogicalCamera},\n")
            sb.append("      \"physicalCameraIds\": [${c.physicalCameraIds.joinToString(",") { "\"${escape(it)}\"" }}],\n")
            sb.append("      \"sensorOrientation\": ${c.sensorOrientation},\n")
            sb.append("      \"sensorActiveArraySize\": \"${escape(c.sensorActiveArraySize)}\",\n")
            sb.append("      \"focalLengths\": [${c.focalLengths.joinToString(",")}],\n")
            sb.append("      \"autoFocusStatus\": \"${c.autoFocusStatus}\",\n")
            sb.append("      \"manualFocusStatus\": \"${c.manualFocusStatus}\",\n")
            sb.append("      \"manualSensorStatus\": \"${c.manualSensorStatus}\",\n")
            sb.append("      \"rawStatus\": \"${c.rawStatus}\",\n")
            sb.append("      \"flashStatus\": \"${c.flashStatus}\",\n")
            sb.append("      \"hdrStatus\": \"${c.hdrStatus}\",\n")
            sb.append("      \"minZoom\": ${c.minZoomRatio},\n")
            sb.append("      \"maxZoom\": ${c.maxZoomRatio},\n")
            if (c.isoRange != null) {
                sb.append("      \"isoRange\": {\"min\": ${c.isoRange.lower}, \"max\": ${c.isoRange.upper}},\n")
            }
            if (c.exposureTimeRangeNs != null) {
                sb.append("      \"exposureTimeNs\": {\"min\": ${c.exposureTimeRangeNs.lower}, \"max\": ${c.exposureTimeRangeNs.upper}},\n")
            }
            sb.append("      \"photoSizes\": [${c.supportedPhotoSizes.take(5).joinToString(",") { "{\"w\": ${it.width}, \"h\": ${it.height}}" }}],\n")
            sb.append("      \"videoSizes\": [${c.supportedVideoSizes.take(5).joinToString(",") { "{\"w\": ${it.width}, \"h\": ${it.height}}" }}],\n")
            sb.append("      \"fpsRanges\": [${c.supportedFpsRanges.joinToString(",") { "{\"lower\": ${it.lower}, \"upper\": ${it.upper}}" }}]\n")
            sb.append("    }${if (i < cameraProfiles.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }
}
