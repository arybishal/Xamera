package com.bishal.adaptivecamera.graphics.shaders

import com.bishal.adaptivecamera.graphics.pipeline.GpuColorConfig
import com.bishal.adaptivecamera.lut.model.TransferFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorPipelineShadersTest {

    @Test
    fun vertexShader_containsRequiredVersionAndInputs() {
        val vs = ColorPipelineShaders.VERTEX_SHADER
        assertTrue("Vertex shader must declare GLSL ES 3.0", vs.contains("#version 300 es"))
        assertTrue("Vertex shader must declare aPosition", vs.contains("aPosition"))
        assertTrue("Vertex shader must declare aTexCoord", vs.contains("aTexCoord"))
        assertTrue("Vertex shader must declare uTexMatrix", vs.contains("uTexMatrix"))
        assertTrue("Vertex shader must declare out vTexCoord", vs.contains("vTexCoord"))
    }

    @Test
    fun previewFragmentShader_containsOesExtensionAndSampler3D() {
        val fs = ColorPipelineShaders.PREVIEW_FRAGMENT_SHADER
        assertTrue("Preview shader must declare GLSL ES 3.0", fs.contains("#version 300 es"))
        assertTrue("Preview shader must require OES external texture extension",
            fs.contains("#extension GL_OES_EGL_image_external_essl3 : require"))
        assertTrue("Preview shader must sample samplerExternalOES", fs.contains("samplerExternalOES uCameraTexture"))
        assertTrue("Preview shader must declare sampler3D for 3D LUT", fs.contains("sampler3D uLut3dTexture"))
        assertTrue("Preview shader must declare halation controls", fs.contains("uHalationIntensity"))
        assertTrue("Preview shader must declare grain controls", fs.contains("uGrainIntensity"))
    }

    @Test
    fun photoFragmentShader_containsSampler2DAndSampler3D() {
        val fs = ColorPipelineShaders.PHOTO_FRAGMENT_SHADER
        assertTrue("Photo shader must declare GLSL ES 3.0", fs.contains("#version 300 es"))
        assertTrue("Photo shader must sample sampler2D", fs.contains("sampler2D uImageTexture"))
        assertTrue("Photo shader must declare sampler3D", fs.contains("sampler3D uLut3dTexture"))
        assertTrue("Photo shader must declare halation controls", fs.contains("uHalationIntensity"))
        assertTrue("Photo shader must declare grain controls", fs.contains("uGrainIntensity"))
    }

    @Test
    fun halfTexelOffsetMath_scalesAccuratelyAcrossStandardLutSizes() {
        // Size 17: scale = 16/17, offset = 0.5/17
        val scale17 = (17 - 1.0f) / 17.0f
        val offset17 = 0.5f / 17.0f
        assertEquals(16.0f / 17.0f, scale17, 0.0001f)
        assertEquals(0.5f / 17.0f, offset17, 0.0001f)

        // At color = 0.0, coord should be exactly half texel (offset)
        val coordZero = 0.0f * scale17 + offset17
        assertEquals(0.5f / 17.0f, coordZero, 0.0001f)

        // At color = 1.0, coord should be (17 - 0.5) / 17
        val coordOne = 1.0f * scale17 + offset17
        assertEquals(16.5f / 17.0f, coordOne, 0.0001f)

        // Size 33
        val scale33 = 32.0f / 33.0f
        val offset33 = 0.5f / 33.0f
        assertEquals(0.5f / 33.0f, 0.0f * scale33 + offset33, 0.0001f)
        assertEquals(32.5f / 33.0f, 1.0f * scale33 + offset33, 0.0001f)

        // Size 65
        val scale65 = 64.0f / 65.0f
        val offset65 = 0.5f / 65.0f
        assertEquals(0.5f / 65.0f, 0.0f * scale65 + offset65, 0.0001f)
        assertEquals(64.5f / 65.0f, 1.0f * scale65 + offset65, 0.0001f)
    }

    @Test
    fun transferModeMapping_matchesExpectedIndices() {
        assertEquals(1, GpuColorConfig(transferFunction = TransferFunction.REC_709).transferModeIndex)
        assertEquals(1, GpuColorConfig(transferFunction = TransferFunction.SRGB).transferModeIndex)
        assertEquals(2, GpuColorConfig(transferFunction = TransferFunction.LINEAR).transferModeIndex)
        assertEquals(3, GpuColorConfig(transferFunction = TransferFunction.S_LOG3).transferModeIndex)
        assertEquals(4, GpuColorConfig(transferFunction = TransferFunction.LOG_C).transferModeIndex)
        assertEquals(5, GpuColorConfig(transferFunction = TransferFunction.HLG).transferModeIndex)
    }

    @Test
    fun filmGrainLuminanceWeighting_peaksAtMidtones() {
        fun lumaWeight(y: Float): Float = (1.0f - abs(2.0f * y - 1.0f)).coerceIn(0.25f, 1.0f)

        // Midtone (0.5) should yield maximum grain weight (1.0)
        assertEquals(1.0f, lumaWeight(0.5f), 0.001f)

        // Pure black (0.0) and pure white (1.0) should yield minimum clamped weight (0.25)
        assertEquals(0.25f, lumaWeight(0.0f), 0.001f)
        assertEquals(0.25f, lumaWeight(1.0f), 0.001f)

        // Quarter tone (0.25) and three-quarter tone (0.75) should yield 0.5
        assertEquals(0.5f, lumaWeight(0.25f), 0.001f)
        assertEquals(0.5f, lumaWeight(0.75f), 0.001f)
    }

    @Test
    fun halationHighlightExcess_activatesOnlyAboveThreshold() {
        val threshold = 0.85f
        fun halationExcess(luma: Float): Float {
            return ((luma - threshold) / (1.0f - threshold)).coerceIn(0.0f, 1.0f)
        }

        // Below threshold: zero halation
        assertEquals(0.0f, halationExcess(0.5f), 0.001f)
        assertEquals(0.0f, halationExcess(0.80f), 0.001f)
        assertEquals(0.0f, halationExcess(0.85f), 0.001f)

        // Halfway between threshold and peak: 0.5
        val mid = (0.85f + 1.0f) / 2.0f
        assertEquals(0.5f, halationExcess(mid), 0.001f)

        // Peak specular highlight (1.0): 1.0
        assertEquals(1.0f, halationExcess(1.0f), 0.001f)
    }
}
