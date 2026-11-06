package com.bishal.adaptivecamera.graphics.opengl

import android.graphics.Bitmap
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages an OpenGL ES Framebuffer Object (FBO) with attached render texture.
 */
class GlFrameBuffer(
    val width: Int,
    val height: Int
) {
    var fboId: Int = 0
        private set
    val renderTexture = GlTexture2D()

    init {
        renderTexture.allocateEmpty(width, height)

        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            renderTexture.textureId,
            0
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            release()
            throw RuntimeException("Framebuffer incomplete, status: 0x${Integer.toHexString(status)}")
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, width, height)
    }

    fun unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    /**
     * Reads pixels from this FBO into a software [Bitmap].
     */
    fun readToBitmap(): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        bind()
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        unbind()
        buffer.position(0)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    fun release() {
        renderTexture.release()
        if (fboId != 0) {
            val fbos = intArrayOf(fboId)
            GLES30.glDeleteFramebuffers(1, fbos, 0)
            fboId = 0
        }
    }
}
