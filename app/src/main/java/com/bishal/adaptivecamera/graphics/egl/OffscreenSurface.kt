package com.bishal.adaptivecamera.graphics.egl

/**
 * Offscreen EGL pbuffer surface used for headless photo processing or GL context warming.
 */
class OffscreenSurface(
    eglCore: EglCore,
    width: Int,
    height: Int
) : EglSurfaceBase(eglCore) {

    init {
        eglSurface = eglCore.createOffscreenSurface(width, height)
        this.width = width
        this.height = height
    }

    fun release() {
        releaseEglSurface()
    }
}
