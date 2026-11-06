package com.bishal.adaptivecamera.camera.config

/**
 * Composition grid overlay options for the viewfinder.
 */
enum class CompositionGridType(
    val displayName: String,
    val description: String
) {
    OFF("Off", "No composition overlay"),
    RULE_OF_THIRDS("Rule of Thirds", "Standard 3x3 grid with lines at 1/3 and 2/3"),
    GOLDEN_RATIO("Golden Ratio", "Phi grid with lines at 0.382 and 0.618"),
    GOLDEN_SPIRAL("Golden Spiral", "Logarithmic Fibonacci golden spiral"),
    CENTER_CROSSHAIRS("Center Crosshairs", "Subtle center target reticle and orthogonal tick marks");

    fun next(): CompositionGridType = when (this) {
        OFF -> RULE_OF_THIRDS
        RULE_OF_THIRDS -> GOLDEN_RATIO
        GOLDEN_RATIO -> GOLDEN_SPIRAL
        GOLDEN_SPIRAL -> CENTER_CROSSHAIRS
        CENTER_CROSSHAIRS -> OFF
    }
}
