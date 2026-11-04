package com.bishal.adaptivecamera.intelligence.scene

/**
 * Categorization of photographic scenes supported by Adaptive Camera intelligence.
 */
enum class SceneCategory(
    val displayName: String,
    val iconEmoji: String,
    val description: String
) {
    STANDARD(
        displayName = "Auto / Standard",
        iconEmoji = "📷",
        description = "Balanced ambient scene with standard exposure parameters"
    ),
    NIGHT(
        displayName = "Night Scene",
        iconEmoji = "🌙",
        description = "Low-light environment requiring exposure stacking and noise suppression"
    ),
    BACKLIT(
        displayName = "Backlit / High Dynamic Range",
        iconEmoji = "☀️",
        description = "Strong background lighting requiring HDR shadow recovery and highlight preservation"
    ),
    PORTRAIT(
        displayName = "Portrait",
        iconEmoji = "👤",
        description = "Human subject or focal object with depth separation"
    ),
    LANDSCAPE(
        displayName = "Landscape",
        iconEmoji = "🏞️",
        description = "Expansive outdoor or architectural composition with deep depth of field"
    ),
    MACRO(
        displayName = "Macro / Close-up",
        iconEmoji = "🔍",
        description = "Subject in extreme close range requiring shallow focus precision"
    ),
    ACTION_MOVEMENT(
        displayName = "Action / Movement",
        iconEmoji = "⚡",
        description = "Fast subject movement or camera motion requiring high-speed shutter priority"
    )
}
