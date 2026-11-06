package com.bishal.adaptivecamera.graphics.pipeline

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import com.bishal.adaptivecamera.graphics.opengl.GlFrameBuffer
import com.bishal.adaptivecamera.graphics.opengl.GlProgram
import com.bishal.adaptivecamera.graphics.opengl.GlTexture2D
import com.bishal.adaptivecamera.graphics.opengl.GlTexture3D
import com.bishal.adaptivecamera.graphics.shaders.ColorPipelineShaders
import com.bishal.adaptivecamera.lut.model.LutDefinition
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Authoritative single GPU color processing pipeline rendering engine.
 * Coordinates OpenGL ES 3.0 shaders for live camera preview, offscreen photo grading,
 * 3D LUT sampling, film grain, and halation.
 */
class GpuRenderPipeline {

    private var previewProgram: GlProgram? = null
    private var photoProgram: GlProgram? = null
    val lutTexture = GlTexture3D()

    private var vertexBuffer: FloatBuffer
    private var texCoordBuffer: FloatBuffer

    var config: GpuColorConfig = GpuColorConfig.DEFAULT
        private set

    init {
        val quadVertices = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        val quadTexCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(quadTexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadTexCoords)
        texCoordBuffer.position(0)

        initShaders()
    }

    private fun initShaders() {
        try {
            previewProgram = GlProgram(
                ColorPipelineShaders.VERTEX_SHADER,
                ColorPipelineShaders.PREVIEW_FRAGMENT_SHADER
            )
            photoProgram = GlProgram(
                ColorPipelineShaders.VERTEX_SHADER,
                ColorPipelineShaders.PHOTO_FRAGMENT_SHADER
            )
            Log.d(TAG, "Color pipeline shader programs linked successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize shaders: ${e.message}", e)
            throw e
        }
    }

    fun updateConfig(newConfig: GpuColorConfig) {
        this.config = newConfig.clamped()
    }

    fun loadLutDefinition(definition: LutDefinition) {
        lutTexture.loadFromDefinition(definition)
    }

    /**
     * Draws a live camera preview frame from an OES external texture ([cameraTextureId])
     * onto the currently bound render surface.
     */
    fun drawPreview(
        cameraTextureId: Int,
        texMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int,
        timeSec: Float = 0.0f
    ) {
        val program = previewProgram ?: return
        program.use()

        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)

        // 1. Bind Camera OES Texture to Unit 0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        program.setUniform1i("uCameraTexture", 0)

        // 2. Bind 3D LUT Texture to Unit 1
        val isLutActive = config.lutEnabled && lutTexture.textureId != 0
        if (isLutActive) {
            lutTexture.bind(1)
            program.setUniform1i("uLut3dTexture", 1)
            program.setUniform1i("uLutEnabled", 1)
            program.setUniform1f("uLutIntensity", config.lutIntensity)
            program.setUniform1f("uLutScale", lutTexture.lutScale)
            program.setUniform1f("uLutOffset", lutTexture.lutOffset)
        } else {
            program.setUniform1i("uLutEnabled", 0)
        }

        // 3. Set Color Science & Effect Uniforms
        program.setUniformMatrix4fv("uTexMatrix", texMatrix)
        program.setUniform1i("uTransferMode", config.transferModeIndex)
        program.setUniform2f("uResolution", viewportWidth.toFloat(), viewportHeight.toFloat())
        program.setUniform1f("uTime", timeSec)

        // Film Grain
        val isGrainActive = config.grainEnabled && config.grainIntensity > 0.0f
        program.setUniform1i("uGrainEnabled", if (isGrainActive) 1 else 0)
        if (isGrainActive) {
            program.setUniform1f("uGrainIntensity", config.grainIntensity)
            program.setUniform1f("uGrainSize", config.grainSize)
        }

        // Halation
        val isHalationActive = config.halationEnabled && config.halationIntensity > 0.0f
        program.setUniform1i("uHalationEnabled", if (isHalationActive) 1 else 0)
        if (isHalationActive) {
            program.setUniform1f("uHalationIntensity", config.halationIntensity)
            program.setUniform1f("uHalationThreshold", config.halationThreshold)
            program.setUniform1f("uHalationRadius", config.halationRadius)
        }

        // 4. Render Fullscreen Quad
        drawQuad()

        // Unbind textures
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        if (isLutActive) {
            lutTexture.unbind(1)
        }
    }

    /**
     * Renders a still photo [sourceBitmap] through the identical color science pipeline
     * offscreen using an FBO and returns the graded [Bitmap].
     */
    fun renderPhoto(sourceBitmap: Bitmap): Bitmap {
        val program = photoProgram ?: return sourceBitmap
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val fbo = GlFrameBuffer(width, height)
        val inputTex = GlTexture2D()
        inputTex.loadFromBitmap(sourceBitmap)

        fbo.bind()
        program.use()

        // 1. Identity texture matrix
        val identityMatrix = FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }
        program.setUniformMatrix4fv("uTexMatrix", identityMatrix)

        // 2. Bind 2D Image Texture to Unit 0
        inputTex.bind(0)
        program.setUniform1i("uImageTexture", 0)

        // 3. Bind 3D LUT Texture to Unit 1
        val isLutActive = config.lutEnabled && lutTexture.textureId != 0
        if (isLutActive) {
            lutTexture.bind(1)
            program.setUniform1i("uLut3dTexture", 1)
            program.setUniform1i("uLutEnabled", 1)
            program.setUniform1f("uLutIntensity", config.lutIntensity)
            program.setUniform1f("uLutScale", lutTexture.lutScale)
            program.setUniform1f("uLutOffset", lutTexture.lutOffset)
        } else {
            program.setUniform1i("uLutEnabled", 0)
        }

        // 4. Set Uniforms
        program.setUniform1i("uTransferMode", config.transferModeIndex)
        program.setUniform2f("uResolution", width.toFloat(), height.toFloat())
        program.setUniform1f("uTime", 0.0f) // Deterministic for still images

        val isGrainActive = config.grainEnabled && config.grainIntensity > 0.0f
        program.setUniform1i("uGrainEnabled", if (isGrainActive) 1 else 0)
        if (isGrainActive) {
            program.setUniform1f("uGrainIntensity", config.grainIntensity)
            program.setUniform1f("uGrainSize", config.grainSize)
        }

        val isHalationActive = config.halationEnabled && config.halationIntensity > 0.0f
        program.setUniform1i("uHalationEnabled", if (isHalationActive) 1 else 0)
        if (isHalationActive) {
            program.setUniform1f("uHalationIntensity", config.halationIntensity)
            program.setUniform1f("uHalationThreshold", config.halationThreshold)
            program.setUniform1f("uHalationRadius", config.halationRadius)
        }

        // 5. Draw
        drawQuad()

        // 6. Read back pixels to bitmap
        val outputBitmap = fbo.readToBitmap()

        // Cleanup temporary render resources
        fbo.unbind()
        fbo.release()
        inputTex.release()
        if (isLutActive) {
            lutTexture.unbind(1)
        }

        return outputBitmap
    }

    private fun drawQuad() {
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    fun release() {
        previewProgram?.release()
        previewProgram = null
        photoProgram?.release()
        photoProgram = null
        lutTexture.release()
    }

    companion object {
        private const val TAG = "GpuRenderPipeline"
    }
}
