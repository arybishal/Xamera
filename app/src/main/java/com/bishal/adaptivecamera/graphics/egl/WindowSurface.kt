package com.bishal.adaptivecamera.graphics.egl

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * Recordable or viewable EGL surface backed by an Android [Surface] or [SurfaceTexture].
 */
class WindowSurface : EglSurfaceBase {
    private var surface: Surface? = null
    private var releaseSurface: Boolean = false

    constructor(eglCore: EglCore, surface: Surface, releaseSurface: Boolean = false) : super(eglCore) {
        eglSurface = eglCore.createWindowSurface(surface)
        this.surface = surface
        this.releaseSurface = releaseSurface
    }

    constructor(eglCore: EglCore, surfaceTexture: SurfaceTexture) : super(eglCore) {
        eglSurface = eglCore.createWindowSurface(surfaceTexture)
    }

    fun release() {
        releaseEglSurface()
        if (releaseSurface && surface != null) {
            surface?.release()
            surface = null
        }
    }
}
