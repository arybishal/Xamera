package com.bishal.adaptivecamera.video.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportAudioActionTest {

    @Test
    fun audioActionEntries_haveNonEmptyDescriptions() {
        VideoExportAudioAction.entries.forEach { action ->
            assertNotNull(action.displayName)
            assertTrue(action.displayName.isNotBlank())
            assertNotNull(action.description)
            assertTrue(action.description.isNotBlank())
        }
    }

    @Test
    fun audioAction_hasExpectedValues() {
        assertEquals("Keep Audio", VideoExportAudioAction.KEEP.displayName)
        assertEquals("Mute Audio", VideoExportAudioAction.MUTE.displayName)
        assertEquals("Extract Audio Only", VideoExportAudioAction.EXTRACT_ONLY.displayName)
    }
}
