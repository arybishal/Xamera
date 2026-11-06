package com.bishal.adaptivecamera.ui.camera.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Front camera screen flash illumination overlay.
 * Temporarily overrides display brightness to 100% and provides soft warm white fill light for selfies.
 */
@Composable
fun ScreenFlashOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    flashColor: Color = Color(0xFFFFF7ED) // Warm soft white
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(isActive) {
        val window = activity?.window ?: return@LaunchedEffect
        val layoutParams = window.attributes
        if (isActive) {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = layoutParams
        } else {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            val layoutParams = window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(flashColor)
        )
    }
}
