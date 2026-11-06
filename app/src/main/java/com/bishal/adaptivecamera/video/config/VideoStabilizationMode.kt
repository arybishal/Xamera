package com.bishal.adaptivecamera.video.config

/**
 * Supported video stabilization modes distinguishing optical hardware (OIS)
 * from software electronic (EIS) stabilization.
 */
enum class VideoStabilizationMode(val label: String, val shortLabel: String, val description: String) {
    OFF("Stabilization Off", "OFF", "Raw sensor readout without stabilization"),
    OIS("Optical (OIS)", "OIS", "Hardware lens-shift stabilization without crop"),
    EIS("Electronic (EIS)", "EIS", "Software gyro-assisted frame stabilization with slight crop"),
    COMBINED("Combined (OIS + EIS)", "COMBO", "Maximum stabilization merging hardware lens shift and software gyro correction");

    val isStabilized: Boolean get() = this != OFF
}
