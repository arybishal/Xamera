package com.bishal.adaptivecamera.graphics.opengl

import android.opengl.GLES30
import android.util.Log
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Manages an OpenGL ES 3.0 3D texture ([GLES30.GL_TEXTURE_3D]) for hardware-accelerated
 * trilinear color grading.
 */
class GlTexture3D {
    var textureId: Int = 0
        private set
    var lutSize: Int = 0
        private set

    /**
     * Scaling factor for half-texel coordinate compensation: (N - 1) / N
     */
    val lutScale: Float
        get() = if (lutSize > 0) (lutSize - 1.0f) / lutSize.toFloat() else 1.0f

    /**
     * Offset factor for half-texel coordinate compensation: 0.5 / N
     */
    val lutOffset: Float
        get() = if (lutSize > 0) 0.5f / lutSize.toFloat() else 0.0f

    /**
     * Uploads the given [definition] into a [GLES30.GL_TEXTURE_3D].
     */
    fun loadFromDefinition(definition: LutDefinition) {
        val metadata = definition.metadata
        require(metadata.type == LutType.LUT_3D) { "GlTexture3D only supports 3D LUTs" }
        require(metadata.size in 2..65) { "LUT size ${metadata.size} exceeds safe bounds [2..65]" }

        lutSize = metadata.size
        val tableData = definition.tableData
        val expectedFloats = lutSize * lutSize * lutSize * 3
        require(tableData.size == expectedFloats) {
            "Table data size (${tableData.size}) does not match expected size ($expectedFloats)"
        }

        // Allocate direct native float buffer
        val floatBuffer: FloatBuffer = ByteBuffer.allocateDirect(tableData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(tableData)
        floatBuffer.position(0)

        if (textureId == 0) {
            val textures = IntArray(1)
            GLES30.glGenTextures(1, textures, 0)
            textureId = textures[0]
            if (textureId == 0) {
                throw RuntimeException("Failed to generate GL 3D texture ID")
            }
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, textureId)

        // Set hardware trilinear interpolation filtering
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)

        // Upload 3D image slice data (OpenGL ES 3.0)
        // Internal format: GL_RGB16F for high precision half-float dynamic range
        GLES30.glTexImage3D(
            GLES30.GL_TEXTURE_3D,
            0,
            GLES30.GL_RGB16F,
            lutSize,
            lutSize,
            lutSize,
            0,
            GLES30.GL_RGB,
            GLES30.GL_FLOAT,
            floatBuffer
        )

        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
        Log.d(TAG, "Loaded 3D LUT Texture ID $textureId (dimension $lutSize^3)")
    }

    fun bind(textureUnit: Int) {
        if (textureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, textureId)
        }
    }

    fun unbind(textureUnit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
    }

    fun release() {
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES30.glDeleteTextures(1, textures, 0)
            textureId = 0
            lutSize = 0
        }
    }

    companion object {
        private const val TAG = "GlTexture3D"
    }
}
