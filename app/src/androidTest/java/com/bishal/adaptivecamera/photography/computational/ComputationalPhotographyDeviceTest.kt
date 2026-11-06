package com.bishal.adaptivecamera.photography.computational

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bishal.adaptivecamera.photography.portrait.PortraitAperture
import com.bishal.adaptivecamera.photography.portrait.PortraitBokehRenderer
import com.bishal.adaptivecamera.photography.portrait.PortraitConfig
import com.bishal.adaptivecamera.photography.portrait.PortraitLightingStyle
import com.bishal.adaptivecamera.photography.portrait.SubjectSegmentationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ComputationalPhotographyDeviceTest {

    @Test
    fun testFrameAlignmentEngine_estimatesDisplacementCorrectly() {
        // Create 200x200 reference pattern
        val ref = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvasRef = Canvas(ref)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvasRef.drawColor(Color.DKGRAY)
        canvasRef.drawRect(50f, 50f, 150f, 150f, paint)

        // Create target with pattern shifted by 6 pixels in X and 4 pixels in Y
        val target = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvasTgt = Canvas(target)
        canvasTgt.drawColor(Color.DKGRAY)
        canvasTgt.drawRect(56f, 54f, 156f, 154f, paint)

        val disp = FrameAlignmentEngine.estimateDisplacement(ref, target, maxShiftPx = 16)

        // Verify shift displacement is estimated accurately
        assertTrue("Estimated dx=${disp.dx} should be close to 6", abs(disp.dx - 6) <= 2)
        assertTrue("Estimated dy=${disp.dy} should be close to 4", abs(disp.dy - 4) <= 2)

        val aligned = FrameAlignmentEngine.alignBitmap(target, disp.dx, disp.dy)
        assertNotNull(aligned)
        assertEquals(200, aligned.width)
        assertEquals(200, aligned.height)

        ref.recycle()
        target.recycle()
        if (aligned !== target) aligned.recycle()
    }

    @Test
    fun testMotionEstimationEngine_detectsMotionMask() {
        val ref = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val target = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)

        // Fill both with identical grey
        val canvasRef = Canvas(ref)
        canvasRef.drawColor(Color.GRAY)
        val canvasTgt = Canvas(target)
        canvasTgt.drawColor(Color.GRAY)

        // Draw a bright moving block in target only
        val paint = Paint().apply { color = Color.WHITE }
        canvasTgt.drawRect(40f, 40f, 80f, 80f, paint)

        val motionMask = MotionEstimationEngine.estimateMotion(ref, target, motionThreshold = 0.1f)

        assertNotNull(motionMask)
        assertTrue("Motion ratio should be positive", motionMask.motionRatio > 0.05f)

        // Center pixel should have high motion weight
        val centerWeight = MotionEstimationEngine.sampleMotionWeight(motionMask, 60, 60, 120, 120)
        assertTrue("Center weight ($centerWeight) should indicate motion", centerWeight > 0.5f)

        // Corner pixel should have near-zero motion weight
        val cornerWeight = MotionEstimationEngine.sampleMotionWeight(motionMask, 5, 5, 120, 120)
        assertTrue("Corner weight ($cornerWeight) should indicate static", cornerWeight < 0.2f)

        ref.recycle()
        target.recycle()
    }

    @Test
    fun testImageFusionEngine_fusesLowLightAndHdrModes() {
        val f1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val f2 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val f3 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        Canvas(f1).drawColor(Color.rgb(60, 60, 60))
        Canvas(f2).drawColor(Color.rgb(70, 70, 70))
        Canvas(f3).drawColor(Color.rgb(65, 65, 65))

        val frames = listOf(f1, f2, f3)
        val dummyMask = MotionMask(1, 1, floatArrayOf(0f), 0f)
        val masks = listOf(dummyMask, dummyMask, dummyMask)

        // Test Low-Light fusion
        val fusedLowLight = ImageFusionEngine.fuseFrames(frames, masks, ImageStackingMode.LOW_LIGHT)
        assertNotNull(fusedLowLight)
        assertEquals(100, fusedLowLight.width)
        assertEquals(100, fusedLowLight.height)

        // Test HDR fusion
        val fusedHdr = ImageFusionEngine.fuseFrames(frames, masks, ImageStackingMode.HDR)
        assertNotNull(fusedHdr)
        assertEquals(100, fusedHdr.width)
        assertEquals(100, fusedHdr.height)

        // Test Detail Enhancement fusion
        val fusedDetail = ImageFusionEngine.fuseFrames(frames, masks, ImageStackingMode.DETAIL_ENHANCEMENT)
        assertNotNull(fusedDetail)
        assertEquals(100, fusedDetail.width)
        assertEquals(100, fusedDetail.height)

        f1.recycle()
        f2.recycle()
        f3.recycle()
        fusedLowLight.recycle()
        fusedHdr.recycle()
        fusedDetail.recycle()
    }

    @Test
    fun testPortraitBokehRenderer_rendersApertureBlurAndLighting() {
        val source = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(source)
        canvas.drawColor(Color.BLUE)

        // Draw center subject
        val paint = Paint().apply { color = Color.rgb(230, 190, 160) }
        canvas.drawCircle(60f, 60f, 30f, paint)

        val config = PortraitConfig(
            aperture = PortraitAperture.F1_4,
            lightingStyle = PortraitLightingStyle.STUDIO
        )

        val rendered = PortraitBokehRenderer.renderPortrait(source, config)
        assertNotNull(rendered)
        assertEquals(120, rendered.width)
        assertEquals(120, rendered.height)

        // Render with Dramatic lighting and F5.6
        val dramaticConfig = PortraitConfig(
            aperture = PortraitAperture.F5_6,
            lightingStyle = PortraitLightingStyle.DRAMATIC
        )
        val renderedDramatic = PortraitBokehRenderer.renderPortrait(source, dramaticConfig)
        assertNotNull(renderedDramatic)
        assertEquals(120, renderedDramatic.width)
        assertEquals(120, renderedDramatic.height)

        source.recycle()
        rendered.recycle()
        renderedDramatic.recycle()
    }
}
