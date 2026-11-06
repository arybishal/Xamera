package com.bishal.adaptivecamera.photography.capture

/**
 * Supported capture output formats for Xamera still photography.
 */
enum class CaptureFormat(
    val id: String,
    val displayName: String,
    val isRaw: Boolean,
    val isProcessed: Boolean,
    val description: String,
    val shortLabel: String
) {
    JPEG(
        id = "JPEG",
        displayName = "JPEG",
        isRaw = false,
        isProcessed = true,
        description = "Standard compressed 8-bit processed JPEG image",
        shortLabel = "JPG"
    ),
    RAW_ONLY(
        id = "RAW_ONLY",
        displayName = "RAW (DNG)",
        isRaw = true,
        isProcessed = false,
        description = "Uncompressed native sensor Bayer stream saved as Adobe DNG",
        shortLabel = "RAW"
    ),
    RAW_JPEG(
        id = "RAW_JPEG",
        displayName = "RAW + JPEG",
        isRaw = true,
        isProcessed = true,
        description = "Synchronized simultaneous dual-format capture sharing identical sensor timestamp",
        shortLabel = "RAW+JPG"
    ),
    RAW_HEIC(
        id = "RAW_HEIC",
        displayName = "RAW + HEIC",
        isRaw = true,
        isProcessed = true,
        description = "Synchronized RAW DNG + high-efficiency 10-bit HEIC compressed capture",
        shortLabel = "RAW+HEIC"
    ),
    HEIC_ONLY(
        id = "HEIC_ONLY",
        displayName = "HEIC",
        isRaw = false,
        isProcessed = true,
        description = "High-efficiency image container offering superior compression",
        shortLabel = "HEIC"
    );

    companion object {
        fun fromId(id: String): CaptureFormat {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: JPEG
        }
    }
}
