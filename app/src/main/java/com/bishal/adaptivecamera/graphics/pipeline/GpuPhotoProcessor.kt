package com.bishal.adaptivecamera.graphics.pipeline

import android.graphics.Bitmap
import android.util.Log
import com.bishal.adaptivecamera.graphics.egl.EglCore
import com.bishal.adaptivecamera.graphics.egl.OffscreenSurface
import com.bishal.adaptivecamera.lut.model.LutDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Headless offscreen GPU photo grading processor.
 * Applies the identical OpenGL ES 3.0 shader graph (3D LUT, Film Grain, Halation)
 * to captured still photo bitmaps.
 */
class GpuPhotoProcessor {

    /**
     * Grades [sourceBitmap] using the GPU shader pipeline under the provided [config]
     * and optional [lutDefinition].
     */
    suspend fun processPhoto(
        sourceBitmap: Bitmap,
        config: GpuColorConfig,
        lutDefinition: LutDefinition? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        // If all color effects are disabled, return original bitmap with zero overhead
        if (!config.lutEnabled && !config.grainEnabled && !config.halationEnabled) {
            return@withContext sourceBitmap
        }

        var eglCore: EglCore? = null
        var offscreenSurface: OffscreenSurface? = null
        var pipeline: GpuRenderPipeline? = null

        try {
            val core = EglCore(flags = 0)
            eglCore = core

            // 1x1 pbuffer is sufficient because rendering target is an FBO with its own texture
            val surface = OffscreenSurface(core, 1, 1)
            surface.makeCurrent()
            offscreenSurface = surface

            val pipe = GpuRenderPipeline()
            pipeline = pipe

            pipe.updateConfig(config)
            if (lutDefinition != null && config.lutEnabled) {
                pipe.loadLutDefinition(lutDefinition)
            }

            val gradedBitmap = pipe.renderPhoto(sourceBitmap)
            Log.d(TAG, "Successfully processed photo on GPU (${sourceBitmap.width}x${sourceBitmap.height})")
            return@withContext gradedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "GPU photo processing failed, returning original: ${e.message}", e)
            return@withContext sourceBitmap
        } finally {
            try {
                pipeline?.release()
                offscreenSurface?.release()
                eglCore?.release()
            } catch (ignored: Exception) {}
        }
    }

    companion object {
        private const val TAG = "GpuPhotoProcessor"
    }
}
