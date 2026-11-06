package com.bishal.adaptivecamera.graphics.egl

import android.opengl.EGL14
import android.opengl.EGLSurface

/**
 * Common base class for all EGL surface wrappers (window and offscreen).
 */
open class EglSurfaceBase protected constructor(protected val eglCore: EglCore) {
    protected var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    var width: Int = -1
        protected set
    var height: Int = -1
        protected set

    fun makeCurrent() {
        eglCore.makeCurrent(eglSurface)
    }

    fun makeCurrentReadFrom(readSurface: EglSurfaceBase) {
        eglCore.makeCurrent(eglSurface, readSurface.eglSurface)
    }

    fun swapBuffers(): Boolean {
        return eglCore.swapBuffers(eglSurface)
    }

    fun setPresentationTime(nsecs: Long) {
        eglCore.setPresentationTime(eglSurface, nsecs)
    }

    fun releaseEglSurface() {
        eglCore.releaseSurface(eglSurface)
        eglSurface = EGL14.EGL_NO_SURFACE
        width = -1
        height = -1
    }
}
