package com.bishal.adaptivecamera.photography.computational

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-level coordinator for multi-frame computational photography.
 * Manages frame alignment, ghost rejection motion masks, and multi-frame image fusion.
 */
class ComputationalPhotographyCoordinator(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager
) {
    companion object {
        private const val TAG = "ComputationalCoordinator"
    }

    /**
     * Processes a burst sequence of [frames] into a single high-fidelity fused bitmap
     * and saves it to MediaStore.
     *
     * @param frames List of captured bitmaps in capture order (first frame is reference).
     * @param config The computational photography configuration.
     * @return Saved [MediaItem] of the fused photo.
     */
    suspend fun processAndSaveStackedPhoto(
        frames: List<Bitmap>,
        config: ImageStackingConfig
    ): MediaItem? = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) {
            Log.e(TAG, "Cannot process empty frames list")
            return@withContext null
        }

        if (frames.size == 1) {
            Log.d(TAG, "Single frame provided, saving directly without fusion")
            return@withContext saveBitmapToMediaStore(frames[0], "IMG_SINGLE")
        }

        val refFrame = frames[0]
        Log.d(TAG, "Starting computational stacking: ${frames.size} frames, mode=${config.mode}")

        val alignedFrames = mutableListOf<Bitmap>()
        alignedFrames.add(refFrame)

        // 1. Align all subsequent frames to the reference frame
        for (i in 1 until frames.size) {
            val target = frames[i]
            val disp = FrameAlignmentEngine.estimateDisplacement(
                reference = refFrame,
                target = target,
                maxShiftPx = config.alignmentMaxShiftPx
            )
            val aligned = FrameAlignmentEngine.alignBitmap(target, disp.dx, disp.dy)
            alignedFrames.add(aligned)
        }

        // 2. Estimate motion and generate ghost rejection masks
        val motionMasks = mutableListOf<MotionMask>()
        for (i in 0 until alignedFrames.size) {
            if (i == 0) {
                // Identity dummy mask for reference frame
                motionMasks.add(
                    MotionMask(
                        width = 1,
                        height = 1,
                        mask = floatArrayOf(0f),
                        motionRatio = 0f
                    )
                )
            } else {
                val mask = MotionEstimationEngine.estimateMotion(
                    reference = refFrame,
                    alignedTarget = alignedFrames[i],
                    motionThreshold = config.motionRejectionThreshold
                )
                motionMasks.add(mask)
            }
        }

        // 3. Multi-frame fusion
        val fusedBitmap = ImageFusionEngine.fuseFrames(
            alignedFrames = alignedFrames,
            motionMasks = motionMasks,
            mode = config.mode,
            referenceIndex = 0
        )

        // 4. Optionally save original burst frames if requested
        if (config.retainOriginalFrames) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            for (i in frames.indices) {
                saveBitmapToMediaStore(
                    frames[i],
                    "IMG_STACK_${timestamp}_frame${i + 1}of${frames.size}"
                )
            }
        }

        // 5. Clean up intermediate aligned frames
        for (i in 1 until alignedFrames.size) {
            if (alignedFrames[i] !== frames[i]) {
                alignedFrames[i].recycle()
            }
        }

        // 6. Save the fused result to MediaStore
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "IMG_STACK_${config.mode.name}_$timestamp"
        val savedItem = saveBitmapToMediaStore(fusedBitmap, filename)

        fusedBitmap.recycle()

        // 7. Recycle original input burst frames to prevent native memory exhaustion
        for (f in frames) {
            if (!f.isRecycled) {
                f.recycle()
            }
        }

        Log.d(TAG, "Computational stacking complete. Saved item: ${savedItem?.uri}")
        savedItem
    }

    /**
     * Encodes [bitmap] to JPEG, optionally injects [exifAttributes], and persists to MediaStore.
     */
    suspend fun saveBitmapToMediaStore(
        bitmap: Bitmap,
        baseFilename: String,
        exifAttributes: Map<String, String>? = null
    ): MediaItem? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "${baseFilename}_temp.jpg")
        try {
            val fos = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            if (exifAttributes != null && exifAttributes.isNotEmpty()) {
                com.bishal.adaptivecamera.photography.exif.ExifMetadataHelper.writeExifMetadata(tempFile, exifAttributes)
            } else {
                com.bishal.adaptivecamera.photography.exif.ExifMetadataHelper.stampDefaultCameraMetadata(tempFile)
            }

            val mediaItem = mediaStoreManager.savePhoto(tempFile, "$baseFilename.jpg")
            mediaItem
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to MediaStore: ${e.message}", e)
            null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
