package com.bishal.adaptivecamera.graphics.opengl

import android.opengl.GLES30
import android.util.Log

/**
 * Utility for compiling OpenGL ES shaders with detailed diagnostic error reporting.
 */
object GlShader {
    private const val TAG = "GlShader"

    fun compileShader(shaderType: Int, source: String): Int {
        val shader = GLES30.glCreateShader(shaderType)
        if (shader == 0) {
            throw RuntimeException("Could not create shader of type: $shaderType")
        }

        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val infoLog = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            val typeStr = when (shaderType) {
                GLES30.GL_VERTEX_SHADER -> "VERTEX_SHADER"
                GLES30.GL_FRAGMENT_SHADER -> "FRAGMENT_SHADER"
                else -> "SHADER_TYPE_$shaderType"
            }
            Log.e(TAG, "Compilation failed for $typeStr: $infoLog\nSource:\n$source")
            throw RuntimeException("Shader compilation failed for $typeStr: $infoLog")
        }

        return shader
    }
}
