package com.bishal.adaptivecamera.camera.capability

data class StreamSizeInfo(
    val width: Int,
    val height: Int,
    val minFrameDurationNs: Long? = null,
    val stallDurationNs: Long? = null,
    val maxFps: Double? = null
) {
    val megapixels: Double
        get() = (width.toLong() * height.toLong()) / 1_000_000.0

    val aspectRatio: String
        get() {
            fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
            val d = gcd(width, height)
            val w = width / d
            val h = height / d
            return if ((w == 16 && h == 9) || (w == 4 && h == 3) || (w == 1 && h == 1)) {
                "$w:$h"
            } else {
                String.format("%.2f:1", width.toDouble() / height)
            }
        }

    val displayString: String
        get() {
            val fpsStr = maxFps?.let { " @ ${String.format("%.0f", it)}fps max" } ?: ""
            val stallStr = if (stallDurationNs != null && stallDurationNs > 0) {
                " (stall: ${stallDurationNs / 1_000_000}ms)"
            } else ""
            return "${width}x${height} (${aspectRatio}, ${String.format("%.1f", megapixels)}MP)$fpsStr$stallStr"
        }
}

data class FormatStreamConfiguration(
    val format: Int,
    val formatName: String,
    val isRaw: Boolean,
    val sizes: List<StreamSizeInfo> = emptyList(),
    val highResolutionSizes: List<StreamSizeInfo> = emptyList()
)

data class StreamCombinationCapability(
    val name: String,
    val description: String,
    val status: CapabilityStatus,
    val guaranteedByHardwareLevel: Boolean,
    val reason: String? = null
)
