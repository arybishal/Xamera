package com.bishal.adaptivecamera.graphics.opengl

import android.opengl.GLES30
import android.util.Log

/**
 * OpenGL ES program manager linking vertex and fragment shaders.
 * Caches uniform locations and provides uniform mutation utilities.
 */
class GlProgram(
    vertexShaderSource: String,
    fragmentShaderSource: String
) {
    var programId: Int = 0
        private set

    private val uniformLocations = mutableMapOf<String, Int>()

    init {
        val vertexShader = GlShader.compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = GlShader.compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)

        programId = GLES30.glCreateProgram()
        if (programId == 0) {
            throw RuntimeException("Could not create GL program")
        }

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val infoLog = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            programId = 0
            Log.e(TAG, "Could not link program: $infoLog")
            throw RuntimeException("Could not link program: $infoLog")
        }

        // Flag individual shaders for deletion once program is released
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun use() {
        if (programId != 0) {
            GLES30.glUseProgram(programId)
        }
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            val loc = GLES30.glGetUniformLocation(programId, name)
            loc
        }
    }

    fun getAttribLocation(name: String): Int {
        return GLES30.glGetAttribLocation(programId, name)
    }

    fun setUniform1i(name: String, value: Int) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1i(loc, value)
    }

    fun setUniform1f(name: String, value: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform1f(loc, value)
    }

    fun setUniform2f(name: String, x: Float, y: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform2f(loc, x, y)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform3f(loc, x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniform4f(loc, x, y, z, w)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
            uniformLocations.clear()
        }
    }

    companion object {
        private const val TAG = "GlProgram"
    }
}
