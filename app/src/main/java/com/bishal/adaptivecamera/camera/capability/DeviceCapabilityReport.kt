package com.bishal.adaptivecamera.camera.capability

import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo

data class DeviceCapabilityReport(
    val deviceInfo: DeviceInfo,
    val cameraProfiles: List<CameraCapabilityProfile>,
    val scannedTimestamp: Long = System.currentTimeMillis(),
    val diagnosticWarnings: List<String> = emptyList(),
    val isFromCache: Boolean = false,
    val cachedTimestamp: Long? = null
) {
    fun toFormattedText(): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("       XAMERA HARDWARE INTELLIGENCE REPORT")
        sb.appendLine("==================================================")
        sb.appendLine("Source: ${if (isFromCache) "OFFLINE CACHE (Timestamp: $cachedTimestamp)" else "LIVE HARDWARE SCAN (Timestamp: $scannedTimestamp)"}")
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
            sb.appendLine("==================================================")
            sb.appendLine("CAMERA #${index + 1} [ID: ${cam.cameraId} | ${cam.facingLabel} (raw: ${cam.rawFacing})]")
            sb.appendLine("==================================================")
            sb.appendLine("Hardware Level: ${cam.hardwareLevel.displayName} (raw: ${cam.rawHardwareLevel})")
            sb.appendLine("Relationship: ${cam.relationshipInfo.relationship} (${cam.relationshipInfo.description})")
            if (cam.relationshipInfo.physicalSubCameraIds.isNotEmpty()) {
                sb.appendLine("Physical Camera IDs: ${cam.relationshipInfo.physicalSubCameraIds.joinToString(", ")}")
            }
            if (cam.relationshipInfo.logicalParentIds.isNotEmpty()) {
                sb.appendLine("Logical Parent Camera IDs: [${cam.relationshipInfo.logicalParentIds.joinToString(", ")}]")
            }

            // SENSOR
            sb.appendLine()
            sb.appendLine("[1. Sensor Specifications]")
            sb.appendLine("Active Array: ${cam.sensorInfo.activeArraySize}")
            sb.appendLine("Pixel Array: ${cam.sensorInfo.pixelArraySize}")
            sb.appendLine("Physical Dimensions: ${cam.sensorInfo.physicalSizeMm}")
            sb.appendLine("Orientation: ${cam.sensorInfo.sensorOrientation}°")
            sb.appendLine("Timestamp Source: ${cam.sensorInfo.timestampSource}")
            if (cam.sensorInfo.colorFilterArrangement != null) {
                sb.appendLine("Color Filter Arrangement: ${cam.sensorInfo.colorFilterArrangement}")
            }
            if (cam.sensorInfo.whiteLevel != null) {
                sb.appendLine("White Level: ${cam.sensorInfo.whiteLevel}")
            }
            if (cam.sensorInfo.blackLevelPattern != null) {
                sb.appendLine("Black Level Pattern: ${cam.sensorInfo.blackLevelPattern}")
            }

            // LENS
            sb.appendLine()
            sb.appendLine("[2. Lens Specifications]")
            if (cam.lensInfo.focalLengths.isNotEmpty()) {
                sb.appendLine("Focal Lengths: ${cam.lensInfo.focalLengths.joinToString(", ") { "${it}mm" }}")
            }
            if (cam.lensInfo.availableApertures.isNotEmpty()) {
                sb.appendLine("Apertures: ${cam.lensInfo.availableApertures.joinToString(", ") { "f/$it" }}")
            }
            if (cam.lensInfo.minimumFocusDistance != null) {
                sb.appendLine("Min Focus Distance: ${cam.lensInfo.minimumFocusDistance} diopters")
            }
            if (cam.lensInfo.hyperfocalDistance != null) {
                sb.appendLine("Hyperfocal Distance: ${cam.lensInfo.hyperfocalDistance} diopters")
            }
            sb.appendLine("Optical Stabilization (OIS): ${if (cam.lensInfo.opticalStabilizationSupported) "SUPPORTED" else "UNSUPPORTED"}")
            if (cam.lensInfo.focusDistanceCalibration != null) {
                sb.appendLine("Focus Calibration: ${cam.lensInfo.focusDistanceCalibration}")
            }

            // MANUAL CONTROLS
            sb.appendLine()
            sb.appendLine("[3. Manual Controls]")
            sb.appendLine("Manual Sensor: ${cam.manualControlInfo.manualSensorStatus}")
            if (cam.manualControlInfo.isoRange != null) {
                sb.appendLine("ISO Range: ${cam.manualControlInfo.isoRange.lower} - ${cam.manualControlInfo.isoRange.upper}")
            }
            if (cam.manualControlInfo.exposureTimeRangeNs != null) {
                sb.appendLine("Exposure Time Range: ${cam.exposureTimeRangeDisplay}")
            }
            if (cam.manualControlInfo.exposureCompensationRange != null && cam.manualControlInfo.exposureCompensationStep != null) {
                val minEv = cam.manualControlInfo.exposureCompensationRange.lower * cam.manualControlInfo.exposureCompensationStep
                val maxEv = cam.manualControlInfo.exposureCompensationRange.upper * cam.manualControlInfo.exposureCompensationStep
                sb.appendLine("Exposure Comp: [${minEv}EV, ${maxEv}EV] step ${cam.manualControlInfo.exposureCompensationStep}EV")
            }
            sb.appendLine("AE Lock: ${cam.manualControlInfo.aeLockStatus} | AWB Lock: ${cam.manualControlInfo.awbLockStatus}")
            sb.appendLine("Manual Focus: ${cam.manualControlInfo.manualFocusStatus}")
            sb.appendLine("Manual White Balance: ${cam.manualControlInfo.mwbStatus}")

            // ZOOM
            sb.appendLine()
            sb.appendLine("[4. Zoom System]")
            sb.appendLine("Zoom Range: ${cam.zoomCapabilityInfo.minZoomRatio}x to ${cam.zoomCapabilityInfo.maxZoomRatio}x")
            sb.appendLine("Max Digital Zoom: ${cam.zoomCapabilityInfo.maxDigitalZoom}x")
            sb.appendLine("Zoom Ratio Range API (Android 11+): ${if (cam.zoomCapabilityInfo.zoomRatioRangeSupported) "SUPPORTED" else "UNSUPPORTED"}")

            // RAW
            sb.appendLine()
            sb.appendLine("[5. RAW Stream Capabilities]")
            sb.appendLine("RAW Status: ${cam.rawCapabilityInfo.status}")
            sb.appendLine("Max RAW Resolution: ${cam.rawCapabilityInfo.maxRawResolution ?: "None"}")
            if (cam.rawCapabilityInfo.supportedRawFormats.isNotEmpty()) {
                val formatNames = cam.rawCapabilityInfo.supportedRawFormats.map { StreamConfigurationAnalyzer.getFormatName(it) }
                sb.appendLine("RAW Formats: ${formatNames.joinToString(", ")}")
            }

            // DYNAMIC RANGE / HDR
            sb.appendLine()
            sb.appendLine("[6. Dynamic Range & HDR]")
            sb.appendLine("HDR Status: ${cam.dynamicRangeInfo.status}")
            if (cam.dynamicRangeInfo.recommendedHdrProfile != null) {
                sb.appendLine("Recommended HDR: ${cam.dynamicRangeInfo.recommendedHdrProfile}")
            }
            if (cam.dynamicRangeInfo.supportedProfiles.isNotEmpty()) {
                sb.appendLine("Available Profiles:")
                cam.dynamicRangeInfo.supportedProfiles.forEach { p ->
                    sb.appendLine("  * ${p.profileName} (${p.bitDepth}-bit) -> ${p.description}")
                }
            }

            // VIDEO
            sb.appendLine()
            sb.appendLine("[7. Video Capabilities]")
            sb.appendLine("Max Video Resolution: ${cam.videoCapabilityInfo.maxVideoResolution ?: "None"}")
            if (cam.videoCapabilityInfo.supportedQualities.isNotEmpty()) {
                sb.appendLine("Supported Standard Qualities:")
                cam.videoCapabilityInfo.supportedQualities.forEach { q ->
                    val fps = q.maxFps?.let { " @ ${String.format("%.0f", it)}fps" } ?: ""
                    sb.appendLine("  * ${q.qualityName}: ${q.resolution.width}x${q.resolution.height}$fps")
                }
            }
            sb.appendLine("Video Stabilization (EIS): ${if (cam.videoCapabilityInfo.supportsVideoStabilization) "SUPPORTED" else "UNSUPPORTED"}")
            sb.appendLine("Preview Stabilization: ${if (cam.videoCapabilityInfo.supportsPreviewStabilization) "SUPPORTED" else "UNSUPPORTED"}")
            if (cam.videoCapabilityInfo.supportedFpsRanges.isNotEmpty()) {
                sb.appendLine("Target FPS Ranges: ${cam.videoCapabilityInfo.supportedFpsRanges.joinToString(", ") { it.toString() }}")
            }

            // STREAM CONFIGURATIONS & DURATIONS
            sb.appendLine()
            sb.appendLine("[8. Stream Configurations & Durations]")
            cam.streamConfigurations.forEach { cfg ->
                val topSizes = cfg.sizes.take(4).joinToString("; ") { it.displayString }
                sb.appendLine("Format: ${cfg.formatName} (${cfg.sizes.size} sizes)")
                if (topSizes.isNotEmpty()) {
                    sb.appendLine("  Top: $topSizes")
                }
                if (cfg.highResolutionSizes.isNotEmpty()) {
                    val highResStr = cfg.highResolutionSizes.take(2).joinToString("; ") { it.displayString }
                    sb.appendLine("  High-Res: $highResStr")
                }
            }

            // STREAM COMBINATIONS
            if (cam.streamCombinations.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("[9. Validated Stream Combinations]")
                cam.streamCombinations.forEach { combo ->
                    sb.appendLine("  * [${combo.status}] ${combo.name}: ${combo.description} (${combo.reason ?: ""})")
                }
            }

            // HARDWARE VALIDATION MATRIX
            sb.appendLine()
            sb.appendLine("[10. Hardware Validation Matrix]")
            sb.appendLine("Advertised Features (${cam.validationSummary.usableAdvertisedCount}/${cam.validationSummary.totalAdvertised} Supported):")
            cam.validationSummary.advertisedFeatures.forEach { (feat, status) ->
                sb.appendLine("  - $feat: $status")
            }
            if (cam.validationSummary.validatedConfigurations.isNotEmpty()) {
                sb.appendLine("Validated Configurations (${cam.validationSummary.validatedCount}):")
                cam.validationSummary.validatedConfigurations.forEach { (cfg, status) ->
                    sb.appendLine("  - $cfg: $status")
                }
            }

            // DIAGNOSTIC NOTES
            if (cam.diagnosticNotes.isNotEmpty()) {
                sb.appendLine()
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
        sb.append("  \"isFromCache\": $isFromCache,\n")
        if (cachedTimestamp != null) {
            sb.append("  \"cachedTimestamp\": $cachedTimestamp,\n")
        }
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
            sb.append("      \"relationship\": {\n")
            sb.append("        \"type\": \"${c.relationshipInfo.relationship}\",\n")
            sb.append("        \"description\": \"${escape(c.relationshipInfo.description)}\",\n")
            sb.append("        \"physicalSubCameraIds\": [${c.relationshipInfo.physicalSubCameraIds.joinToString(",") { "\"${escape(it)}\"" }}],\n")
            sb.append("        \"logicalParentIds\": [${c.relationshipInfo.logicalParentIds.joinToString(",") { "\"${escape(it)}\"" }}]\n")
            sb.append("      },\n")
            sb.append("      \"sensorInfo\": {\n")
            sb.append("        \"activeArraySize\": \"${escape(c.sensorInfo.activeArraySize)}\",\n")
            sb.append("        \"pixelArraySize\": \"${escape(c.sensorInfo.pixelArraySize)}\",\n")
            sb.append("        \"physicalSizeMm\": \"${escape(c.sensorInfo.physicalSizeMm)}\",\n")
            sb.append("        \"sensorOrientation\": ${c.sensorInfo.sensorOrientation},\n")
            sb.append("        \"timestampSource\": \"${escape(c.sensorInfo.timestampSource)}\",\n")
            sb.append("        \"whiteLevel\": ${c.sensorInfo.whiteLevel ?: "null"},\n")
            sb.append("        \"blackLevelPattern\": \"${escape(c.sensorInfo.blackLevelPattern ?: "null")}\",\n")
            sb.append("        \"colorFilterArrangement\": \"${escape(c.sensorInfo.colorFilterArrangement ?: "null")}\"\n")
            sb.append("      },\n")
            sb.append("      \"lensInfo\": {\n")
            sb.append("        \"focalLengths\": [${c.lensInfo.focalLengths.joinToString(",")}],\n")
            sb.append("        \"availableApertures\": [${c.lensInfo.availableApertures.joinToString(",")}],\n")
            sb.append("        \"minimumFocusDistance\": ${c.lensInfo.minimumFocusDistance ?: "null"},\n")
            sb.append("        \"hyperfocalDistance\": ${c.lensInfo.hyperfocalDistance ?: "null"},\n")
            sb.append("        \"opticalStabilizationSupported\": ${c.lensInfo.opticalStabilizationSupported}\n")
            sb.append("      },\n")
            sb.append("      \"manualControlInfo\": {\n")
            sb.append("        \"manualSensorStatus\": \"${c.manualControlInfo.manualSensorStatus}\",\n")
            sb.append("        \"manualFocusStatus\": \"${c.manualControlInfo.manualFocusStatus}\",\n")
            sb.append("        \"aeLockStatus\": \"${c.manualControlInfo.aeLockStatus}\",\n")
            sb.append("        \"awbLockStatus\": \"${c.manualControlInfo.awbLockStatus}\",\n")
            sb.append("        \"mwbStatus\": \"${c.manualControlInfo.mwbStatus}\",\n")
            if (c.manualControlInfo.isoRange != null) {
                sb.append("        \"isoRange\": {\"min\": ${c.manualControlInfo.isoRange.lower}, \"max\": ${c.manualControlInfo.isoRange.upper}},\n")
            }
            if (c.manualControlInfo.exposureTimeRangeNs != null) {
                sb.append("        \"exposureTimeNs\": {\"min\": ${c.manualControlInfo.exposureTimeRangeNs.lower}, \"max\": ${c.manualControlInfo.exposureTimeRangeNs.upper}},\n")
            }
            sb.append("        \"supportsManualSensor\": ${c.manualControlInfo.supportsManualSensor}\n")
            sb.append("      },\n")
            sb.append("      \"zoomCapabilityInfo\": {\n")
            sb.append("        \"minZoomRatio\": ${c.zoomCapabilityInfo.minZoomRatio},\n")
            sb.append("        \"maxZoomRatio\": ${c.zoomCapabilityInfo.maxZoomRatio},\n")
            sb.append("        \"maxDigitalZoom\": ${c.zoomCapabilityInfo.maxDigitalZoom},\n")
            sb.append("        \"zoomRatioRangeSupported\": ${c.zoomCapabilityInfo.zoomRatioRangeSupported}\n")
            sb.append("      },\n")
            sb.append("      \"rawCapabilityInfo\": {\n")
            sb.append("        \"status\": \"${c.rawCapabilityInfo.status}\",\n")
            sb.append("        \"supportsRawSensor\": ${c.rawCapabilityInfo.supportsRawSensor},\n")
            sb.append("        \"maxRawResolution\": ${c.rawCapabilityInfo.maxRawResolution?.let { "{\"w\": ${it.width}, \"h\": ${it.height}}" } ?: "null"}\n")
            sb.append("      },\n")
            sb.append("      \"dynamicRangeInfo\": {\n")
            sb.append("        \"status\": \"${c.dynamicRangeInfo.status}\",\n")
            sb.append("        \"recommendedHdrProfile\": \"${escape(c.dynamicRangeInfo.recommendedHdrProfile ?: "null")}\",\n")
            sb.append("        \"profiles\": [${c.dynamicRangeInfo.supportedProfiles.joinToString(",") { "{\"name\": \"${escape(it.profileName)}\", \"bitDepth\": ${it.bitDepth}, \"isHdr\": ${it.isHdr}}" }}]\n")
            sb.append("      },\n")
            sb.append("      \"videoCapabilityInfo\": {\n")
            sb.append("        \"supportsVideoStabilization\": ${c.videoCapabilityInfo.supportsVideoStabilization},\n")
            sb.append("        \"supportsPreviewStabilization\": ${c.videoCapabilityInfo.supportsPreviewStabilization},\n")
            sb.append("        \"maxVideoResolution\": ${c.videoCapabilityInfo.maxVideoResolution?.let { "{\"w\": ${it.width}, \"h\": ${it.height}}" } ?: "null"},\n")
            sb.append("        \"supportedQualities\": [${c.videoCapabilityInfo.supportedQualities.joinToString(",") { "{\"quality\": \"${escape(it.qualityName)}\", \"w\": ${it.resolution.width}, \"h\": ${it.resolution.height}}" }}]\n")
            sb.append("      },\n")
            sb.append("      \"streamConfigurations\": [\n")
            c.streamConfigurations.forEachIndexed { sIdx, cfg ->
                sb.append("        {\n")
                sb.append("          \"format\": ${cfg.format},\n")
                sb.append("          \"formatName\": \"${escape(cfg.formatName)}\",\n")
                sb.append("          \"isRaw\": ${cfg.isRaw},\n")
                sb.append("          \"sizes\": [${cfg.sizes.take(5).joinToString(",") { "{\"w\": ${it.width}, \"h\": ${it.height}, \"minDurationNs\": ${it.minFrameDurationNs ?: "null"}, \"stallNs\": ${it.stallDurationNs ?: "null"}}" }}]\n")
                sb.append("        }${if (sIdx < c.streamConfigurations.size - 1) "," else ""}\n")
            }
            sb.append("      ],\n")
            sb.append("      \"streamCombinations\": [\n")
            c.streamCombinations.forEachIndexed { cmIdx, combo ->
                sb.append("        {\n")
                sb.append("          \"name\": \"${escape(combo.name)}\",\n")
                sb.append("          \"status\": \"${combo.status}\",\n")
                sb.append("          \"guaranteed\": ${combo.guaranteedByHardwareLevel}\n")
                sb.append("        }${if (cmIdx < c.streamCombinations.size - 1) "," else ""}\n")
            }
            sb.append("      ]\n")
            sb.append("    }${if (i < cameraProfiles.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }
}
