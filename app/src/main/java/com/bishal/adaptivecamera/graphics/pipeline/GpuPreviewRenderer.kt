package com.bishal.adaptivecamera.graphics.pipeline

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.bishal.adaptivecamera.graphics.egl.EglCore
import com.bishal.adaptivecamera.graphics.egl.WindowSurface
import com.bishal.adaptivecamera.lut.model.LutDefinition

/**
 * High-performance, dedicated-thread OpenGL ES 3.0 camera preview renderer.
 * Receives camera frames via an external OES [SurfaceTexture], runs them through
 * [GpuRenderPipeline] (3D LUT, Film Grain, Halation), and swaps to the display [WindowSurface].
 */
class GpuPreviewRenderer(
    private val onFrameRendered: ((fps: Float, frameTimeMs: Float) -> Unit)? = null
) : SurfaceTexture.OnFrameAvailableListener {

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private var eglCore: EglCore? = null
    private var windowSurface: WindowSurface? = null
    private var pipeline: GpuRenderPipeline? = null

    private var cameraTextureId: Int = 0
    private var cameraSurfaceTexture: SurfaceTexture? = null
    var cameraSurface: Surface? = null
        private set

    private val texMatrix = FloatArray(16)
    private var viewportWidth: Int = 1080
    private var viewportHeight: Int = 1920

    private var frameCount: Long = 0
    private var lastFpsTimestamp: Long = 0
    private var startTimeSec: Float = 0f

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(displaySurfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (isRunning) stop()

        viewportWidth = width
        viewportHeight = height

        val thread = HandlerThread("GpuPreviewRenderThread").apply { start() }
        renderThread = thread
        val handler = Handler(thread.looper)
        renderHandler = handler

        handler.post {
            initGl(displaySurfaceTexture)
        }
        isRunning = true
    }

    private fun initGl(displaySurfaceTexture: SurfaceTexture) {
        try {
            val core = EglCore(flags = 0)
            eglCore = core

            val winSurf = WindowSurface(core, displaySurfaceTexture)
            winSurf.makeCurrent()
            windowSurface = winSurf

            val textures = IntArray(1)
            GLES30.glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            val camST = SurfaceTexture(cameraTextureId)
            camST.setOnFrameAvailableListener(this, renderHandler)
            cameraSurfaceTexture = camST
            cameraSurface = Surface(camST)

            pipeline = GpuRenderPipeline()

            startTimeSec = System.currentTimeMillis() / 1000f
            lastFpsTimestamp = System.currentTimeMillis()
            Log.d(TAG, "GL Preview initialized successfully ($viewportWidth x $viewportHeight)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GL Preview: ${e.message}", e)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        renderHandler?.post {
            drawFrame()
        }
    }

    private fun drawFrame() {
        val camST = cameraSurfaceTexture ?: return
        val winSurf = windowSurface ?: return
        val pipe = pipeline ?: return

        val startNs = System.nanoTime()

        try {
            camST.updateTexImage()
            camST.getTransformMatrix(texMatrix)

            val currentTimeSec = (System.currentTimeMillis() / 1000f) - startTimeSec

            winSurf.makeCurrent()
            pipe.drawPreview(cameraTextureId, texMatrix, viewportWidth, viewportHeight, currentTimeSec)
            winSurf.swapBuffers()

            frameCount++
            val nowMs = System.currentTimeMillis()
            val elapsedMs = nowMs - lastFpsTimestamp
            if (elapsedMs >= 1000) {
                val fps = (frameCount * 1000f) / elapsedMs
                val frameTimeMs = (System.nanoTime() - startNs) / 1_000_000f
                onFrameRendered?.invoke(fps, frameTimeMs)
                frameCount = 0
                lastFpsTimestamp = nowMs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Render frame error: ${e.message}")
        }
    }

    fun updateConfig(config: GpuColorConfig) {
        renderHandler?.post {
            pipeline?.updateConfig(config)
        }
    }

    fun loadLutDefinition(definition: LutDefinition) {
        renderHandler?.post {
            pipeline?.loadLutDefinition(definition)
        }
    }

    fun stop() {
        isRunning = false
        val handler = renderHandler
        val thread = renderThread

        handler?.post {
            pipeline?.release()
            pipeline = null

            cameraSurface?.release()
            cameraSurface = null
            cameraSurfaceTexture?.release()
            cameraSurfaceTexture = null

            if (cameraTextureId != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(cameraTextureId), 0)
                cameraTextureId = 0
            }

            windowSurface?.release()
            windowSurface = null
            eglCore?.release()
            eglCore = null

            thread?.quitSafely()
        }

        renderHandler = null
        renderThread = null
    }

    companion object {
        private const val TAG = "GpuPreviewRenderer"
    }
}
