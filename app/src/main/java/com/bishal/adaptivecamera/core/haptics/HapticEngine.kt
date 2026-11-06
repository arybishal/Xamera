package com.bishal.adaptivecamera.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

interface HapticEngine {
    fun performShutter()
    fun performClick()
    fun performTick()
    fun performLevelLock()
    fun performModeSwitch()
}

class HapticEngineImpl(
    private val context: Context,
    private val isHapticsEnabledProvider: () -> Boolean = { true }
) : HapticEngine {

    companion object {
        private const val TAG = "HapticEngine"
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize vibrator service", e)
            null
        }
    }

    private fun isHapticsAllowed(): Boolean {
        if (!isHapticsEnabledProvider()) return false
        val vib = vibrator ?: return false
        if (!vib.hasVibrator()) return false

        return try {
            val systemHaptic = Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            )
            systemHaptic != 0
        } catch (_: Exception) {
            true
        }
    }

    override fun performShutter() {
        if (!isHapticsAllowed()) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(20L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute shutter haptic", e)
        }
    }

    override fun performClick() {
        if (!isHapticsAllowed()) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(15L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute click haptic", e)
        }
    }

    override fun performTick() {
        if (!isHapticsAllowed()) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(8L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute tick haptic", e)
        }
    }

    override fun performLevelLock() {
        if (!isHapticsAllowed()) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(25L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute level lock haptic", e)
        }
    }

    override fun performModeSwitch() {
        if (!isHapticsAllowed()) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(10L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to execute mode switch haptic", e)
        }
    }
}
