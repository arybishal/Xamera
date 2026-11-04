package com.bishal.adaptivecamera.photography.pro

import kotlin.math.roundToLong

data class ShutterPreset(
    val nanoseconds: Long,
    val displayString: String
)

object ShutterSpeedHelper {

    // Standard photographic shutter steps
    val STANDARD_SHUTTER_STEPS: List<ShutterPreset> = listOf(
        ShutterPreset(125_000L, "1/8000s"),
        ShutterPreset(250_000L, "1/4000s"),
        ShutterPreset(500_000L, "1/2000s"),
        ShutterPreset(1_000_000L, "1/1000s"),
        ShutterPreset(2_000_000L, "1/500s"),
        ShutterPreset(4_000_000L, "1/250s"),
        ShutterPreset(8_000_000L, "1/125s"),
        ShutterPreset(16_666_666L, "1/60s"),
        ShutterPreset(33_333_333L, "1/30s"),
        ShutterPreset(66_666_666L, "1/15s"),
        ShutterPreset(125_000_000L, "1/8s"),
        ShutterPreset(250_000_000L, "1/4s"),
        ShutterPreset(500_000_000L, "1/2s"),
        ShutterPreset(1_000_000_000L, "1s"),
        ShutterPreset(2_000_000_000L, "2s"),
        ShutterPreset(4_000_000_000L, "4s"),
        ShutterPreset(8_000_000_000L, "8s"),
        ShutterPreset(15_000_000_000L, "15s"),
        ShutterPreset(30_000_000_000L, "30s")
    )

    fun formatNanoseconds(nanos: Long): String {
        if (nanos <= 0L) return "Auto"
        val seconds = nanos.toDouble() / 1_000_000_000.0
        return if (seconds >= 1.0) {
            if (seconds == seconds.roundToLong().toDouble()) {
                "${seconds.roundToLong()}s"
            } else {
                String.format("%.1fs", seconds)
            }
        } else {
            val fraction = (1.0 / seconds).roundToLong()
            "1/${fraction}s"
        }
    }

    fun getSupportedPresets(minNs: Long, maxNs: Long): List<ShutterPreset> {
        val filtered = STANDARD_SHUTTER_STEPS.filter { it.nanoseconds in minNs..maxNs }.toMutableList()
        // If minNs is faster than 1/8000s (e.g. 78,120ns = ~1/12800s on OnePlus Ace 5 / 13R), include min limit
        if (minNs < (filtered.firstOrNull()?.nanoseconds ?: Long.MAX_VALUE)) {
            filtered.add(0, ShutterPreset(minNs, formatNanoseconds(minNs)))
        }
        // If maxNs is longer than 30s (e.g. 74.35s on OnePlus Ace 5 / 13R), include max limit
        if (maxNs > (filtered.lastOrNull()?.nanoseconds ?: 0L)) {
            filtered.add(ShutterPreset(maxNs, formatNanoseconds(maxNs)))
        }
        return filtered
    }
}
