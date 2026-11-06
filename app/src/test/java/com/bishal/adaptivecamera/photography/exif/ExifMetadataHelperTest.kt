package com.bishal.adaptivecamera.photography.exif

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifMetadataHelperTest {

    @Test
    fun testStandardTagsContainEssentialAttributes() {
        val tags = ExifMetadataHelper.STANDARD_EXIF_TAGS
        assertTrue(tags.contains(ExifInterface.TAG_MAKE))
        assertTrue(tags.contains(ExifInterface.TAG_MODEL))
        assertTrue(tags.contains(ExifInterface.TAG_ORIENTATION))
        assertTrue(tags.contains(ExifInterface.TAG_DATETIME))
        assertTrue(tags.contains(ExifInterface.TAG_EXPOSURE_TIME))
        assertTrue(tags.contains(ExifInterface.TAG_ISO_SPEED_RATINGS))
    }

    @Test
    fun testRotationDegreesFromOrientationTag() {
        assertEquals(0, ExifMetadataHelper.getRotationDegreesFromOrientation(null))
        assertEquals(0, ExifMetadataHelper.getRotationDegreesFromOrientation("1")) // NORMAL
        assertEquals(90, ExifMetadataHelper.getRotationDegreesFromOrientation("6")) // ROTATE_90
        assertEquals(180, ExifMetadataHelper.getRotationDegreesFromOrientation("3")) // ROTATE_180
        assertEquals(270, ExifMetadataHelper.getRotationDegreesFromOrientation("8")) // ROTATE_270
        assertEquals(0, ExifMetadataHelper.getRotationDegreesFromOrientation("invalid"))
    }

    @Test
    fun testRotateNullBitmapReturnsNull() {
        val result = ExifMetadataHelper.rotateBitmapIfRequired(null, 90)
        assertNull(result)
    }
}
