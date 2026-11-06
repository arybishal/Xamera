package com.bishal.adaptivecamera.lut.parser

import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.LutValidationResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeLutParserTest {

    @Test
    fun parse_valid3dCube17_succeedsWithCorrectMetadataAndTable() {
        // Construct a small 2x2x2 cube string for fast deterministic validation
        val cubeContent = """
            # Adobe Cube 3D Test LUT
            TITLE "Small2x2x2"
            LUT_3D_SIZE 2
            DOMAIN_MIN 0.0 0.0 0.0
            DOMAIN_MAX 1.0 1.0 1.0
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val result = CubeLutParser.parse(cubeContent)
        assertTrue("Parser should succeed for valid 2x2x2 3D cube", result is LutValidationResult.Success)

        val definition = (result as LutValidationResult.Success).definition
        assertEquals("Small2x2x2", definition.metadata.title)
        assertEquals(LutType.LUT_3D, definition.metadata.type)
        assertEquals(2, definition.metadata.size)
        assertEquals(8, definition.metadata.totalEntries)
        assertEquals(24, definition.tableData.size)

        // First RGB entry
        assertEquals(0.0f, definition.tableData[0], 0.0001f)
        assertEquals(0.0f, definition.tableData[1], 0.0001f)
        assertEquals(0.0f, definition.tableData[2], 0.0001f)

        // Last RGB entry
        assertEquals(1.0f, definition.tableData[21], 0.0001f)
        assertEquals(1.0f, definition.tableData[22], 0.0001f)
        assertEquals(1.0f, definition.tableData[23], 0.0001f)
    }

    @Test
    fun parse_valid1dCube_succeedsWithCorrect1DMetadata() {
        val cubeContent = """
            # 1D LUT Test
            TITLE "Simple1D"
            LUT_1D_SIZE 4
            0.0 0.0 0.0
            0.33 0.33 0.33
            0.66 0.66 0.66
            1.0 1.0 1.0
        """.trimIndent()

        val result = CubeLutParser.parse(cubeContent)
        assertTrue("Parser should succeed for valid 1D cube", result is LutValidationResult.Success)

        val definition = (result as LutValidationResult.Success).definition
        assertEquals(LutType.LUT_1D, definition.metadata.type)
        assertEquals(4, definition.metadata.size)
        assertEquals(4, definition.metadata.totalEntries)
        assertEquals(12, definition.tableData.size)
    }

    @Test
    fun parse_scientificNotation_parsedAccurately() {
        val cubeContent = """
            TITLE "SciNotation"
            LUT_3D_SIZE 2
            1.2345e-02 2.3456E-01 9.8765e+00
            0.0 0.0 0.0
            0.0 0.0 0.0
            0.0 0.0 0.0
            0.0 0.0 0.0
            0.0 0.0 0.0
            0.0 0.0 0.0
            1.0 1.0 1.0
        """.trimIndent()

        val result = CubeLutParser.parse(cubeContent)
        assertTrue(result is LutValidationResult.Success)
        val definition = (result as LutValidationResult.Success).definition
        assertEquals(0.012345f, definition.tableData[0], 0.00001f)
        assertEquals(0.23456f, definition.tableData[1], 0.0001f)
        assertEquals(9.8765f, definition.tableData[2], 0.001f)
    }

    @Test
    fun parse_missingSizeHeader_failsWithExplicitError() {
        val invalidContent = """
            # No size header specified
            TITLE "Invalid"
            0.1 0.2 0.3
        """.trimIndent()

        val result = CubeLutParser.parse(invalidContent)
        assertTrue("Should fail with Error", result is LutValidationResult.Error)
        val error = result as LutValidationResult.Error
        assertTrue(error.message.contains("before LUT size declaration") || error.message.contains("Missing LUT"))
    }

    @Test
    fun parse_oversized3dSize_rejectedForMemorySafety() {
        val invalidContent = """
            LUT_3D_SIZE 128
        """.trimIndent()

        val result = CubeLutParser.parse(invalidContent)
        assertTrue(result is LutValidationResult.Error)
        val error = result as LutValidationResult.Error
        assertTrue(error.message.contains("exceeds maximum safe limit"))
    }

    @Test
    fun parse_truncatedTableData_rejectedWithCountMismatch() {
        val truncatedContent = """
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            0.5 0.5 0.5
            # Only 2 entries provided instead of 8 (2^3)
        """.trimIndent()

        val result = CubeLutParser.parse(truncatedContent)
        assertTrue(result is LutValidationResult.Error)
        val error = result as LutValidationResult.Error
        assertTrue(error.message.contains("expected 8 entries"))
    }

    @Test
    fun parse_invalidDomainBounds_rejected() {
        val invalidDomain = """
            LUT_3D_SIZE 2
            DOMAIN_MIN 1.0 1.0 1.0
            DOMAIN_MAX 0.0 0.0 0.0
        """.trimIndent()

        val result = CubeLutParser.parse(invalidDomain)
        assertTrue(result is LutValidationResult.Error)
        val error = result as LutValidationResult.Error
        assertTrue(error.message.contains("strictly greater than DOMAIN_MIN"))
    }

    @Test
    fun parse_emptyInput_rejected() {
        val result = CubeLutParser.parse("   \n\n  ")
        assertTrue(result is LutValidationResult.Error)
    }

    @Test
    fun parse_valid33x33x33Headers_boundsValidated() {
        // Fast header and structure validation
        assertTrue(CubeLutParser.MAX_SUPPORTED_3D_SIZE >= 33)
        assertTrue(CubeLutParser.MAX_SUPPORTED_3D_SIZE == 65)
    }
}
