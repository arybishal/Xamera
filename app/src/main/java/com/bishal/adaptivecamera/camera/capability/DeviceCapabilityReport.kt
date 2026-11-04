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
        sb.appendLine("Manufacturer: ${deviceInfo.manufacturer}")
        sb.appendLine("Model: ${deviceInfo.model}")
        sb.appendLine("Device: ${deviceInfo.device}")
        sb.appendLine("Brand: ${deviceInfo.brand}")
        sb.appendLine("Hardware: ${deviceInfo.hardware}")
        sb.appendLine("Android Version: ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
        sb.appendLine("Build ID: ${deviceInfo.displayBuildId}")
        sb.appendLine("Fingerprint: ${deviceInfo.buildFingerprint}")
        sb.appendLine("OnePlus Ace 5 Variant Detected: ${if (deviceInfo.isPrimaryTargetAce5) "YES (Primary Target)" else "NO (Generic/Secondary Target)"}")
        sb.appendLine()
        sb.appendLine("--- CAMERA SUMMARY ---")
        sb.appendLine("Total Accessible Cameras: ${cameraProfiles.size}")
        val backCount = cameraProfiles.count { it.facing == 0 }
        val frontCount = cameraProfiles.count { it.facing == 1 }
        sb.appendLine("Rear Cameras: $backCount | Front Cameras: $frontCount")
        val rawCount = cameraProfiles.count { it.supportsRaw }
        val manualCount = cameraProfiles.count { it.supportsManualSensor }
        sb.appendLine("RAW Capable: $rawCount | Manual Sensor Controls: $manualCount")
        sb.appendLine()

        if (diagnosticWarnings.isNotEmpty()) {
            sb.appendLine("--- DIAGNOSTIC WARNINGS ---")
            diagnosticWarnings.forEach { warning ->
                sb.appendLine("! $warning")
            }
            sb.appendLine()
        }

        cameraProfiles.forEachIndexed { index, cam ->
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("CAMERA #${index + 1} [ID: ${cam.cameraId} | ${cam.facingLabel}]")
            sb.appendLine("--------------------------------------------------")
            sb.appendLine("Hardware Level: ${cam.hardwareLevel.displayName}")
            sb.appendLine("Is Logical Camera: ${cam.isLogicalCamera}")
            if (cam.physicalCameraIds.isNotEmpty()) {
                sb.appendLine("Physical Camera IDs: ${cam.physicalCameraIds.joinToString(", ")}")
            }
            sb.appendLine("Sensor Orientation: ${cam.sensorOrientation}°")
            if (cam.sensorActiveArraySize.isNotEmpty()) {
                sb.appendLine("Active Array: ${cam.sensorActiveArraySize}")
            }
            if (cam.focalLengths.isNotEmpty()) {
                sb.appendLine("Focal Lengths: ${cam.focalLengths.joinToString(", ") { "${it}mm" }}")
            }
            sb.appendLine("Zoom Range: ${cam.minZoomRatio}x - ${cam.maxZoomRatio}x")
            sb.appendLine("Autofocus Supported: ${cam.supportsAutoFocus}")
            sb.appendLine("Manual Focus Supported: ${cam.supportsManualFocus}")
            if (cam.minimumFocusDistance != null) {
                sb.appendLine("Min Focus Distance: ${cam.minimumFocusDistance} diopters")
            }
            sb.appendLine("Manual Sensor (ISO/Shutter): ${cam.supportsManualSensor}")
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
            sb.appendLine("Flash Supported: ${cam.supportsFlash}")
            sb.appendLine("RAW (DNG) Supported: ${cam.supportsRaw}")
            sb.appendLine("HDR Output Profile Support: ${cam.supportsHdr}")
            if (cam.dynamicRangeProfilesSupported.isNotEmpty()) {
                sb.appendLine("Dynamic Range Profiles: ${cam.dynamicRangeProfilesSupported.joinToString(", ")}")
            }

            sb.appendLine("Max Photo Size: ${cam.maxPhotoResolution ?: "None"}")
            val topPhotoSizes = cam.supportedPhotoSizes.take(5).joinToString(", ") { it.toString() }
            sb.appendLine("Sample Photo Sizes: $topPhotoSizes")

            sb.appendLine("Max Video Size: ${cam.maxVideoResolution ?: "None"}")
            val topVideoSizes = cam.supportedVideoSizes.take(5).joinToString(", ") { it.toString() }
            sb.appendLine("Sample Video Sizes: $topVideoSizes")

            val fpsRangesStr = cam.supportedFpsRanges.joinToString(", ") { it.toString() }
            sb.appendLine("FPS Ranges: $fpsRangesStr")
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
        sb.append("    \"manufacturer\": \"${escape(deviceInfo.manufacturer)}\",\n")
        sb.append("    \"model\": \"${escape(deviceInfo.model)}\",\n")
        sb.append("    \"device\": \"${escape(deviceInfo.device)}\",\n")
        sb.append("    \"brand\": \"${escape(deviceInfo.brand)}\",\n")
        sb.append("    \"hardware\": \"${escape(deviceInfo.hardware)}\",\n")
        sb.append("    \"androidVersion\": \"${escape(deviceInfo.androidVersion)}\",\n")
        sb.append("    \"apiLevel\": ${deviceInfo.apiLevel},\n")
        sb.append("    \"buildFingerprint\": \"${escape(deviceInfo.buildFingerprint)}\",\n")
        sb.append("    \"isPrimaryTargetAce5\": ${deviceInfo.isPrimaryTargetAce5}\n")
        sb.append("  },\n")
        sb.append("  \"diagnosticWarnings\": [")
        sb.append(diagnosticWarnings.joinToString(",") { "\"${escape(it)}\"" })
        sb.append("],\n")
        sb.append("  \"cameraProfiles\": [\n")
        cameraProfiles.forEachIndexed { i, c ->
            sb.append("    {\n")
            sb.append("      \"cameraId\": \"${escape(c.cameraId)}\",\n")
            sb.append("      \"facing\": ${c.facing},\n")
            sb.append("      \"facingLabel\": \"${c.facingLabel}\",\n")
            sb.append("      \"hardwareLevel\": \"${c.hardwareLevel.displayName}\",\n")
            sb.append("      \"isLogicalCamera\": ${c.isLogicalCamera},\n")
            sb.append("      \"physicalCameraIds\": [${c.physicalCameraIds.joinToString(",") { "\"${escape(it)}\"" }}],\n")
            sb.append("      \"sensorOrientation\": ${c.sensorOrientation},\n")
            sb.append("      \"focalLengths\": [${c.focalLengths.joinToString(",")}],\n")
            sb.append("      \"supportsAutoFocus\": ${c.supportsAutoFocus},\n")
            sb.append("      \"supportsManualFocus\": ${c.supportsManualFocus},\n")
            sb.append("      \"supportsManualSensor\": ${c.supportsManualSensor},\n")
            sb.append("      \"supportsRaw\": ${c.supportsRaw},\n")
            sb.append("      \"supportsFlash\": ${c.supportsFlash},\n")
            sb.append("      \"supportsHdr\": ${c.supportsHdr},\n")
            sb.append("      \"minZoom\": ${c.minZoomRatio},\n")
            sb.append("      \"maxZoom\": ${c.maxZoomRatio},\n")
            if (c.isoRange != null) {
                sb.append("      \"isoRange\": {\"min\": ${c.isoRange.lower}, \"max\": ${c.isoRange.upper}},\n")
            }
            if (c.exposureTimeRangeNs != null) {
                sb.append("      \"exposureTimeNs\": {\"min\": ${c.exposureTimeRangeNs.lower}, \"max\": ${c.exposureTimeRangeNs.upper}},\n")
            }
            sb.append("      \"photoSizes\": [${c.supportedPhotoSizes.joinToString(",") { "{\"w\": ${it.width}, \"h\": ${it.height}}" }}],\n")
            sb.append("      \"videoSizes\": [${c.supportedVideoSizes.joinToString(",") { "{\"w\": ${it.width}, \"h\": ${it.height}}" }}],\n")
            sb.append("      \"fpsRanges\": [${c.supportedFpsRanges.joinToString(",") { "{\"lower\": ${it.lower}, \"upper\": ${it.upper}}" }}]\n")
            sb.append("    }${if (i < cameraProfiles.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }
}
